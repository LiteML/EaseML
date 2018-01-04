package easeml.dag;

import java.util.List;

/**
 * Created by takun on 02/01/2018.
 */
interface Stage {

    String getName() ;

    List<Stage> getParents() ;

    void preprocess() ;

    Object process(Object ... obj) ;

    void postprocess() ;
}
