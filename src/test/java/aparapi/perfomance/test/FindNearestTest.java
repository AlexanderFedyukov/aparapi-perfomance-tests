package aparapi.perfomance.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aparapi.Kernel.EXECUTION_MODE;
import com.aparapi.device.Device.TYPE;
import com.aparapi.device.OpenCLDevice;

public class FindNearestTest {
	private final static Logger logger = LoggerFactory.getLogger(FindNearestTest.class);


	@Test
	public void testJTP() {
		new FindNearest(null, EXECUTION_MODE.JTP);
	}

	@Test
	public void testCPU() {
		OpenCLDevice.listDevices(TYPE.CPU).forEach(dev -> {
			logger.info("{} - {}", dev.getName(), dev.getShortDescription());

				new FindNearest(dev,
						EXECUTION_MODE.CPU);
		});
	}

	@Test
	public void testGPU0() {
		OpenCLDevice.listDevices(TYPE.GPU).forEach(dev -> {
			logger.info("{} - {}", dev.getName(), dev.getShortDescription());

			// I skip Raven because it hang the system
			if (!dev.getName().contains("RAVEN"))
				new FindNearest(dev,
						EXECUTION_MODE.GPU);
		});
	}


}
