package ytech.prototype.osiris.gateway;

/**
 * Created by XCCubeSSD on 2016/02/08.
 */
public interface HttpPostListener {
    abstract public void postCompletion(byte[] response);
    abstract public void postFialure();
}
