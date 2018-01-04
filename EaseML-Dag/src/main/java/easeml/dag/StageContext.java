package easeml.dag;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by takun on 02/01/2018.
 */
public interface StageContext {
    <T> Future<T> run(StageRunTime stage) ;
    Future<List<Object>> parallel_run(List<StageRunTime> stages) ;
    void start() ;
    void shutdown() ;
}
