package software.glare.cid.process.processes.processor.algorithm;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Created by fdman on 16.07.2014.
 */
public class AlgorithmPoolFactory<T extends IAlgorithm> extends BasePooledObjectFactory<T> {
    private final Class algorithmClass;

    public AlgorithmPoolFactory(Class algorithmClass) {
        this.algorithmClass = algorithmClass;
    }

    @Override
    public T create() throws Exception {
        IAlgorithm algorithm = (IAlgorithm) algorithmClass.newInstance(); // With proper error handling
        return (T) algorithm;
    }

    /**
     * Use the default PooledObject implementation.
     */
    @Override
    public PooledObject<T> wrap(T algorithm) {
        return new DefaultPooledObject<>(algorithm);
    }

    /**
     * When an object is returned to the pool, clear the buffer.
     */
    @Override
    public void passivateObject(PooledObject<T> pooledObject) {
        pooledObject.getObject().clearData();
    }

    // for all other methods, the no-op implementation
    // in BasePooledObjectFactory will suffice

}
