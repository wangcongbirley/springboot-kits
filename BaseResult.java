public abstract class BaseResult {
	
	private boolean flag;//返回成功标志
	
	private String msg; //返回信息

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
