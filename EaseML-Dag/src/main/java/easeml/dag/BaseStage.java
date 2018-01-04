package easeml.dag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by takun on 02/01/2018.
 */
public abstract class BaseStage implements Stage{
    protected Object NONE = null ;
    private List<Stage> parents = null ;

    public BaseStage() {
        this.parents = new ArrayList<>();
    }

    public BaseStage(List<Stage> parents) {
        this.parents = parents ;
    }

    public BaseStage(Stage[] parents) {
        this.parents = Arrays.asList(parents);
    }

    public List<Stage> getParents() {
        return parents;
    }

    public void dependOn(Stage parent) {
        parents.add(parent) ;
    }

    public void dependOn(Stage ... ps) {
        parents.addAll(Arrays.asList(ps)) ;
    }

    @Override
    public void preprocess() {}

    @Override
    public void postprocess() {}
}
