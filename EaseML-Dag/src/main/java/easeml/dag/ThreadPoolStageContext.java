package easeml.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by takun on 02/01/2018.
 */
public class ThreadPoolStageContext implements StageContext{

    class StageCallable<T> implements Callable<T>{
        private Stage stage = null ;
        private Object[] args = null ;
        StageCallable(StageRunTime srt) {
            this.stage = srt.getStage() ;
            this.args = srt.getArgs() ;
        }

        @Override
        public T call() {
            stage.preprocess();
            Object result = stage.process(args);
            stage.postprocess();
            return (T) result ;
        }
    }

    private ExecutorService pool = null ;
    private int thread_num ;

    public ThreadPoolStageContext(int thread_num) {
        this.thread_num = thread_num ;
    }

    public ThreadPoolStageContext() {
        this(10) ;
    }

    @Override
    public <T> Future<T> run(final StageRunTime stage) {
        return pool.submit(new StageCallable<T>(stage)) ;
    }

    @Override
    public Future<List<Object>> parallel_run(final List<StageRunTime> stages) {
        return new FutureTask<>(new Callable<List<Object>>() {
            @Override
            public List<Object> call() throws Exception {
                List<Object> result = new ArrayList<>(stages.size());
                List<Future<Object>> futures = new ArrayList<>(stages.size());
                for(StageRunTime srt: stages) {
                    Future<Object> f = pool.submit(new StageCallable<Object>(srt)) ;
                    futures.add(f) ;
                }
                for (Future<Object> f : futures) {
                    Object r = f.get() ;
                    result.add(r) ;
                }
                return result;
            }
        });
    }

    @Override
    public void start() {
        this.pool = Executors.newFixedThreadPool(thread_num) ;
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }
}
