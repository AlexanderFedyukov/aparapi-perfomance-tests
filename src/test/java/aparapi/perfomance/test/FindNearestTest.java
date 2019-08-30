package aparapi.perfomance.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aparapi.Kernel.EXECUTION_MODE;
import com.aparapi.device.Device.TYPE;
import com.aparapi.internal.kernel.KernelManager;
import com.aparapi.device.OpenCLDevice;

public class FindNearestTest {
    private final static Logger logger = LoggerFactory.getLogger(FindNearestTest.class);
    private static FindNearest worker;

    @BeforeClass
    public static void before() {
        worker = new FindNearest(1<<1);
    }

    @Test
    public void testJTP() {
        worker.compute(null, EXECUTION_MODE.JTP);
    }

    @Test
    public void testCPU() {
        OpenCLDevice.listDevices(TYPE.CPU).forEach(dev -> {
            logger.info("{} - {}", dev.getName(), dev.getShortDescription());

            worker.compute(dev, EXECUTION_MODE.CPU);
        });
    }

    @Test
    public void testGPU() {
        OpenCLDevice.listDevices(TYPE.GPU).forEach(dev -> {
            logger.info("{} - {}", dev.getName(), dev.getShortDescription());

            // I skip Raven because it hang the system
            if (!dev.getName().contains("RAVEN"))
                worker.compute(dev, EXECUTION_MODE.GPU);
        });
    }
    
    @Test
    public void testComplex() {
        for(int i=1;i<20;i++) {
            worker=new FindNearest(1<<i);
            testJTP();
            testCPU();
            testGPU();
        }
    }
    
    @AfterClass
    public static void after() {
        final StringBuilder builder = new StringBuilder();
        KernelManager.instance().reportDeviceUsage(builder, true);
        logger.info("{}", builder);
    }
}
