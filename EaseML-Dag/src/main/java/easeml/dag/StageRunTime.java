package easeml.dag;

/**
 * Created by takun on 02/01/2018.
 */
public class StageRunTime {

    private Stage stage = null ;
    private Object[] args = null ;
    public StageRunTime(Stage stage, Object[] args) {
        this.stage = stage ;
        this.args = args ;
    }

    public Stage getStage() {
        return stage;
    }

    public Object[] getArgs() {
        return args;
    }
}
