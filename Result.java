/**
 * 返回数据封装类
 * @author wangcongbirley
 *
 * @param <T>
 */
public class Result<T> extends BaseResult{
	
	private T data; //数据封装

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
