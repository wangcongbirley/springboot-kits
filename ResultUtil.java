public class ResultUtil {
	
	/**
	 * 添加信息成功
	 * @return
	 */
	public static Result<Object> actionSuccessfullyAndObject(String message,Object object){
		Result<Object> result=new Result<Object>();
		result.setData(object);
		result.setFlag(true);
        result.setMsg(message);
        return result;
	}

	/**
	 * 添加信息成功
	 * @return
	 */
	public static Result<Object> actionSuccessfully(String message){
		Result<Object> result=new Result<Object>();
		result.setFlag(true);
        result.setMsg(message);
        return result;
	}
	/**
	 * 返回失败信息
	 * @param message
	 * @return
	 */
	public static Result<Object> error(String message){
		Result<Object> result=new Result<Object>();
		result.setFlag(false);
        result.setMsg(message);
        return result;
	} 	
}
