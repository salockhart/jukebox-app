package ca.dal.group5.jukefit.API;

/**
 * Created by lockhart on 2017-06-14.
 */

public interface RequestHandler<T> {

    void success(T result);
    void error(int code);
}
