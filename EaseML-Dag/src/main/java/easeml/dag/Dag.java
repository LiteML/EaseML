package easeml.dag;


import java.util.*;
import java.util.concurrent.*;

/**
 * Created by takun on 02/01/2018.
 */
public class Dag {
    private StageContext context = null ;

    public Dag(StageContext context) {
        this.context = context ;
    }

    private List<Stage> stages = new ArrayList<>();
    private Status[] status = null ;
    private boolean[] notified = null ;
    private Future<?>[] results = null;
    private Map<Integer, Set<Integer>> parents = new HashMap<>();
    private Map<Integer, Set<Integer>> children = new HashMap<>();
    private BlockingQueue<Integer> queue = new LinkedBlockingQueue<>() ;
    private boolean isFineshed = false ;
    private Status all_status = Status.UNDEFINED ;
    private Object[] init_args ;

    public Dag addStage(Stage leaf) {
        addAll(leaf) ;
        return this ;
    }

    private void addEdge(Map<Integer, Set<Integer>> map, int key, int value) {
        Set<Integer> children = map.get(key) ;
        if(children == null) {
            children = new LinkedHashSet<>() ;
            map.put(key, children) ;
        }
        children.add(value) ;
    }

    private int addAll(Stage leaf) {
        int idx = stages.indexOf(leaf) ;
        if(idx < 0) {
            idx = stages.size() ;
            stages.add(leaf) ;
        }
        if(leaf.getParents() != null) {
            if(leaf.getParents().size() == 0) {
                addEdge(children, -1, idx);
            }
            for(Stage stage: leaf.getParents()) {
                int pid = addAll(stage) ;
                addEdge(parents, idx, pid);
                addEdge(children, pid, idx);
            }
        }
        return idx ;
    }

    private void log(Object log) {
        System.out.println(log);
    }

    private void reset() {
        status = new Status[stages.size()] ;
        results = new Future<?>[stages.size()] ;
        notified = new boolean[stages.size()] ;
        all_status = Status.UNDEFINED ;
        isFineshed = false ;
    }

    public Status run(Object ... args) {
        init_args = args ;
        queue.add(-1) ;
        start();
        return all_status ;
    }

    /*public void run(Stage stage, Object ... args) {
        run(stages.indexOf(stage), args);
    }*/

    private void run(int idx, Object[] args) {
        Stage stage = stages.get(idx) ;
        StageRunTime srt = new StageRunTime(stage, args) ;
        synchronized(stage) {
            if(all_status == Status.UNDEFINED) all_status = Status.RUNNING ;
            status[idx] = Status.RUNNING ;
            results[idx] = context.run(srt) ;
        }
    }

    private Status parents_status(int idx) {
        Set<Integer> _parents = parents.get(idx) ;
        if(_parents == null) return Status.SUCCESSED ;
        for(int p: _parents) {
            if(status[p] == Status.FAILED) return Status.FAILED ;
            if(status[p] == Status.RUNNING) return Status.RUNNING ;
            if(status[p] == Status.UNDEFINED) return Status.UNDEFINED ;
        }
        return Status.SUCCESSED ;
    }

    private synchronized boolean check_is_finished() {
        if(all_status == Status.UNDEFINED) return isFineshed ;
        if(isFineshed) return true ;
        int runnable_stage = 0 ;
        int running_stage = 0 ;
        int error_stage = 0 ;
        for(int i = 0; i < status.length; i++) {
            synchronized (stages.get(i)) {
                if(status[i] == Status.RUNNING) {
                    running_stage ++ ;
                }else if(status[i] == Status.UNDEFINED) {
                    Status parent_status = parents_status(i) ;
                    if(parent_status == Status.SUCCESSED) {
                        runnable_stage ++ ;
                    }
                }else if(status[i] == Status.FAILED) {
                    error_stage ++ ;
                }
            }
        }
        log("running_stage: " + running_stage + " , failed_stage: " + error_stage);
        if(running_stage > 0) {
            all_status = Status.RUNNING ;
        }else if(runnable_stage > 0) {
            log("xxxx");
        }else if(error_stage > 0) {
            all_status = Status.FAILED ;
            isFineshed = true ;
        }else{
            all_status = Status.SUCCESSED ;
            isFineshed = true ;
        }
        return isFineshed ;
    }

    private void shutdown(){
        log("shutdown");
        context.shutdown();
    }

    private FutureTask<Integer> check_stage_result(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (! isFineshed) {
                    for(int i = 0; i < results.length; i++) {
                        if(results[i] != null && results[i].isDone()) {
                            if (!notified[i]) {
                                notified[i] = true;
                                try {
                                    log("finished_stage : " + stages.get(i).getName());
                                    results[i].get();
                                    status[i] = Status.SUCCESSED;
                                    queue.add(i);
                                } catch(Exception e){
                                    status[i] = Status.FAILED;
                                    e.printStackTrace();
//                                System.out.println("run exception : " + e.getMessage());
                                }
                            }
                        }
                    }
                    check_is_finished() ;
                    synchronized (this) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                log("quit check stage result .") ;
            }
        } ;
        FutureTask<Integer> future = new FutureTask<>(runnable, 0) ;
        new Thread(future).start();
        return future ;
    }

    private void start() {
        if(stages.isEmpty()) {
            log("stages isEmpty, please add stages .") ;
            return ;
        }
        reset();
        log("start");
        context.start() ;
        FutureTask<Integer> stage_future = check_stage_result();
        try{
            while (!isFineshed) {
                Integer idx = -2;
                try {
                    idx = queue.poll(100, TimeUnit.MILLISECONDS);
                    if(idx == null) continue;
                } catch (InterruptedException e) {
                    continue;
                }
                Set<Integer> _children = children.get(idx);
                if(_children != null) {
                    for (int child : _children) {
                        Status parent_status = parents_status(child);
                        if (parent_status == Status.SUCCESSED) {
                            Set<Integer> pids = parents.get(child);
                            Object[] args = init_args ;
                            if(pids != null) {
                                args = new Object[pids.size()];
                                int c = 0;
                                for (int pid : pids) {
                                    args[c++] = results[pid].get();
                                }
                            }
                            Dag.this.run(child, args);
                        }
                    }
                }
            }
            stage_future.get() ;
        }catch (Exception e){
            throw new RuntimeException(e) ;
        }finally {
            shutdown();
        }
    }
}
