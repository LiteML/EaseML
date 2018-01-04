package easeml.dag;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by takun on 03/01/2018.
 */
public enum Status {

    UNDEFINED,
    RUNNING,
    SUCCESSED,
    FAILED,
    ;

    private static Map<Integer, Status> mapping = new HashMap<>();
    static {
        for(Status status: Status.values()){
            mapping.put(status.ordinal(), status) ;
        }
    }

    public static Status valueOf(int ordinal) {
        return mapping.get(ordinal) ;
    }
}
