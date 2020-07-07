import com.huawei.it.sso.filter.util.SsoConstants;
import com.huawei.it.support.usermanage.helper.UserInfoBean;
import com.huawei.mda.service.ThisToolService;
import com.huawei.support.cbb.util.json.JSONUtil;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * 文件上传公共类
 *
 * @author  y00356763
 * @version  V100R001C00, 2017年4月27日
 * @see  [相关类/方法]
 * @since  IDP/子系统名/版本
 */
@WebServlet(name = "fileUploadServlet", urlPatterns = "/fileUploadServlet")
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileUploadServlet.class);

    // 文件上传保存路径
    private static final String FILE_UPLOAD_DIR = "/appfile/demo";

    private static final String RETURN_CODE = "returnCode";

    private static final String RETURN_MSG = "returnMessage";

    // 每天上传文件的总大小管控，单位byte
    private static final long DAILY_MAX_FILESIZE = 2 * 1024 * 1024 * 1024L; // 2G

    // 单个文件的大小管控，单位byte
    private static final long MAXFILESIZE = 200 * 1024 * 1024L; // 200M

    // 允许上传的文件后缀
    private static final String CHECKED_ALL_FILETYPE = "csv,zip";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static ThisToolService thisToolService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        ServletContext servletContext = config.getServletContext();
        ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (springContext != null) {
            try {
                thisToolService = springContext.getBean(ThisToolService.class);
            } catch (BeansException e) {
                LOG.error("Get " + ThisToolService.class + " from spring context failed!");
            }
        } else {
            LOG.error("No spring context.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        FileItem fileItem = null;
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");

            Map<String, String> result = new HashMap<String, String>();
            // 获取用户Id
            String userId = getUserId(request);
            if (StringUtils.isEmpty(userId)) {
                result.put(RETURN_CODE, "500");
                result.put(RETURN_MSG, "The user is not log in!");
                String data = JSONUtil.toString(result);
                IOUtils.write(data, response.getWriter());
                return;
            }

            // TODO 可以根据用户的角色类型校验用户是否有上传文件的权限

            // 获取文件
            fileItem = getFileItem(request);
            // 获取文件名称
            String fileName = getFileName(fileItem);
            LOG.info("[INFO] Scan the fileName: " + fileName);
            // 获取文件大小
            int fileSize = getFileSize(fileItem);
            LOG.info("[INFO] Scan the fileSize: " + fileSize);

            // 1.校验文件
            result = checkAndOperateFile(request, userId, fileItem, fileSize);

            if ("500".equals(result.get(RETURN_CODE))) {
                String data = JSONUtil.toString(result);
                IOUtils.write(data, response.getWriter());
            } else {
                // 2.存储文件
                String filePath = writeFile(fileItem, fileName);

                String fileId = StringUtils.EMPTY;
                fileId = submitFileInfo(filePath, userId, fileSize);

                LOG.info("submitFileInfo fileId:" + fileId);
                result.put("fileId", fileId);
                String data = JSONUtil.toString(result);
                IOUtils.write(data, response.getWriter());
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("FileUploadServlet doPost UnsupportedEncodingException", e);
        } catch (IOException e) {
            LOG.error("FileUploadServlet doPost IOException", e);
        } catch (Exception e) {
            LOG.error("FileUploadServlet doPost Exception", e);
        } catch (Throwable t) {
            LOG.error("FileUploadServlet doPost Throwable", t);
        }
    }

    private String getFileName(FileItem fileItem) throws UnsupportedEncodingException {
        String filename = StringUtils.EMPTY;

        if (fileItem != null) {
            String value = new File(fileItem.getName()).getName();
            if (StringUtils.isNotEmpty(value)) {
                // 修改codecc
                value = Normalizer.normalize(value, Form.NFKC);
                int start = value.lastIndexOf("\\");
                filename = value.substring(start + 1);
                filename = URLDecoder.decode(URLEncoder.encode(filename, "utf-8"), "utf-8");
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                } else {
                    filename = filename.substring(filename.lastIndexOf("\\") + 1);
                }
            }
        }
        return filename;
    }

    /**
     * 获取用户ID
     * @param request
     * @return
     */
    private String getUserId(HttpServletRequest request) {
        String userId = StringUtils.EMPTY;
        // 获取用户登录信息
        HttpSession httpSessn = request.getSession(false);
        if ((httpSessn != null) && (httpSessn.getAttribute(SsoConstants.SESSION_USER_INFO_KEY) != null)) {
            // 从session中获取sso登录的用户信息
            UserInfoBean uiBean = (UserInfoBean) httpSessn.getAttribute(SsoConstants.SESSION_USER_INFO_KEY);
            userId = uiBean.getUid();
        }
        return userId;
    }

    /**
     * 文件上传校验
     * @param request
     * @return
     * @throws Exception [参数说明]
     *
     * @return Map<String,String> [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private Map<String, String> checkAndOperateFile(HttpServletRequest request, String userId, FileItem fileItem,
        int fileSize) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        // 文件上传校验
        Map<String, Object> checkResult = checkFile(request, userId, fileItem, fileSize);
        boolean outcome = (Boolean) checkResult.get("isValid");
        // 1.检查文件是否合规
        if (!outcome) {
            result.put(RETURN_CODE, "500");
            result.put(RETURN_MSG, checkResult.get(RETURN_MSG).toString());
        }
        return result;
    }

    /**
     * 1.获取请求来源的工具路径
     * 2.校验用户是否登录
     * 3.校验工具是否支持上传及内网是否签署免责声明
     * 4.校验文件格式是否为可允许
     * 5.校验是否超过最大限制
     * 5.防暴力：校验文件单日上传总大小
    * @param inputStream
    * @return [参数说明]
    *
    * @return boolean [返回类型说明]
    * @exception throws [违例类型] [违例说明]
    * @see [类、类#方法、类#成员]
    */
    private Map<String, Object> checkFile(HttpServletRequest request, String userId, FileItem fileItem,
        int fileSize) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            if (null != fileItem && StringUtils.isNotEmpty(fileItem.getName())) {
                // 1.校验文件格式是否为可允许
                Map<String, Object> checkFileTypeResult = checkFileType(fileItem);
                if (!isValid(checkFileTypeResult)) {
                    return getMessage(checkFileTypeResult);
                }

                // 2.获取文件大小，并校验是否超过最大限制
                Map<String, Object> checkFileSizeMap = checkFileSize(fileSize);
                if (!isValid(checkFileSizeMap)) {
                    return getMessage(checkFileSizeMap);
                }

                // 3.防暴力：校验文件单日上传总大小
                Map<String, Object> checkTotalResult = checkTotalFileSize(fileSize);
                if (!isValid(checkTotalResult)) {
                    return getMessage(checkTotalResult);
                }
            } else {
                map.put(RETURN_MSG, "File is empty!");
                map.put("isValid", false);
                return map;
            }

        } catch (IOException e) {
            map.put(RETURN_MSG, "File operation failed!");
            map.put("isValid", false);
            return map;
        } catch (Exception e) {
            map.put(RETURN_MSG, "System error!");
            map.put("isValid", false);
            return map;
        }

        map.put("isValid", true);
        return map;
    }

    private Map<String, Object> getMessage(Map<String, Object> mesMap) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RETURN_MSG, mesMap.get(RETURN_MSG));
        map.put("isValid", mesMap.get("isValid"));
        return map;
    }

    /**
     * 获取boolean值
     * @param mesMap
     * @return [参数说明]
     *
     * @return boolean [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private boolean isValid(Map<String, Object> mesMap) {
        return (Boolean) mesMap.get("isValid");
    }

    /**
     * 防暴力：校验文件单日上传总大小（加开关）
     * @return [参数说明]
     *
     * @return boolean [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private Map<String, Object> checkTotalFileSize(int fileSize) {
        // 获取文件单日限额大小
        long fileMaxSize = DAILY_MAX_FILESIZE;
        long size = thisToolService.getDailyFileSize();
        if ((size + fileSize) > fileMaxSize) {
            return getResult("The daily upload amount has exceeded the maximum value!", false);
        }
        return getResult(null, true);
    }

    private Map<String, Object> getResult(String msg, boolean status) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtils.isNotEmpty(msg)) {
            map.put(RETURN_MSG, msg);
        }
        map.put("isValid", status);
        return map;
    }

    /**
     * 校验文件格式是否允许
     * @param fileItem
     * @return [参数说明]
     *
     * @return boolean [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private Map<String, Object> checkFileType(FileItem fileItem) {
        // 获取文件类型(允许上传的所有类型格式,直接以文件命名)
        String allFileType = CHECKED_ALL_FILETYPE;
        try {
            String fileName = fileItem.getName();
            // 文件名不允许包含../ 或者 ..\\ ，如果包含说明是恶意请求
            if (StringUtils.contains(fileName, "../") || StringUtils.contains(fileName, "..\\")) {
                return getResult("The type of file is not allowed!", false);
            }

            // 对文件名称进行标准化处理
            fileName = new File(fileName).getName();

            // 获取文件后缀
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            // 文件后缀不包含在允许上传的格式里面
            List<String> allFileTypes = Arrays.asList(StringUtils.split(allFileType, ","));
            if (allFileTypes == null || !allFileTypes.contains(suffix)) {
                return getResult("The type of file is not allowed!", false);
            }
        } catch (Exception e) {
            LOG.error("checkFileType Error", e);
        }
        return getResult(null, true);
    }

    /**
     * 校验单个上传文件大小
     * @return [参数说明]
     *
     * @return Map<String,Object> [返回类型说明]
     * @throws IOException
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private Map<String, Object> checkFileSize(int fileSize) throws IOException {
        long fileMaxSize = MAXFILESIZE;
        LOG.info("[INFO] Scan the fileMaxSize: " + fileMaxSize);
        if (fileSize > fileMaxSize) {
            return getResult("The file size has exceeded the maximum value!", false);
        }
        return getResult(null, true);
    }

    private int getFileSize(FileItem fileItem) throws IOException {
        // 按字节计算
        InputStream input;
        if (fileItem != null) {
            input = fileItem.getInputStream();
            int fileSize = input.available();
            input.close();
            return fileSize;
        }
        return 0;
    }

    private FileItem getFileItem(HttpServletRequest request) {
        // 配置上传参数
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // 创建一个ServletFileUpload对象
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");

        try {
            ArrayList<FileItem> list = (ArrayList<FileItem>) upload.parseRequest(request);
            for (FileItem fileItem : list) {
                if (!fileItem.isFormField()) {
                    return fileItem;

                }
            }
        } catch (FileUploadException e) {
            LOG.error("getFileItem error", e);
        }

        return null;
    }

    /**
     * 获取文件上传路径
     * {rootPath}/{onlinetoolId}/{日期}
     * @return [参数说明]
     *
     * @return String [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private String getFileUploadDir() {
        UUID uuid = UUID.randomUUID();
        String rootPath = FILE_UPLOAD_DIR;

        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间

        String fileUpload = rootPath + File.separator + date + File.separator + uuid;

        return fileUpload;
    }

    /**
     * 调用Service接口记录文件信息,返回文件Id
     * @param request
     * @param response
     * @return [参数说明]
     * 
     * @return String [返回类型说明]
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private String submitFileInfo(String filePath, String userId, long fileSize) {
        String fileId = "";
        try {
            // UploadFileInfo fileInfo = new UploadFileInfo();
            // fileInfo.setFileId("");
            // fileInfo.setPath(filePath);
            // fileInfo.setFileSize(String.valueOf(fileSize));
            // fileInfo.setStatus("1");
            // fileInfo.setUserId(userId);
            // fileInfo.createTime(userId);
            // fileInfo.updateTime(userId);
            // thisToolService.addOrUpdateFileInfo(fileInfo);
            LOG.info("addOrUpdateFileInfo fileId:" + fileId);
        } catch (Exception e) {
            LOG.error("addOrUpdateFileInfo error in submitFileInfo method.", e);
        }
        return fileId;
    }

    /**
     * 写文件至存储
     * @param request
     * @param response [参数说明]
     *
     * @return void [返回类型说明]
     * @throws Exception
     * @exception throws [违例类型] [违例说明]
     * @see [类、类#方法、类#成员]
     */
    private String writeFile(FileItem fileItem, String fileName) {
        String filePath = StringUtils.EMPTY;
        try {
            if (fileItem != null) {
                if (StringUtils.isNotEmpty(fileName)) {
                    String uploadPath = getFileUploadDir();
                    if (StringUtils.isNotEmpty(uploadPath)) {
                        // 如果目录不存在则创建
                        File uploadDir = FileUtils.getFile(uploadPath);
                        if (uploadDir != null && !uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }
                        try {
                            filePath = uploadPath + File.separator + fileName;
                            File storeFile = FileUtils.getFile(filePath);
                            if (storeFile != null) {
                                // 保存文件到硬盘
                                fileItem.write(storeFile);
                            }
                        } catch (Exception e) {
                            LOG.error("write error in writeFile method.", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("writeFile error.", e);
        }

        return filePath;
    }
}
