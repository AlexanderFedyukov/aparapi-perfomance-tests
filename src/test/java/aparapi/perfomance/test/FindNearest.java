package aparapi.perfomance.test;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aparapi.Kernel;
import com.aparapi.Kernel.EXECUTION_MODE;
import com.aparapi.Range;
import com.aparapi.device.OpenCLDevice;
import com.aparapi.internal.kernel.KernelManager;

public class FindNearest {
	private final static Logger logger = LoggerFactory.getLogger(FindNearest.class);

	private  int					amount = 1 << 20;

	private final KMapReducer kernel = new KMapReducer();

	public FindNearest(int amount) {
	    this.amount=amount;
	    
	    kernel.data = new double[amount][2];
        kernel.res = new double[amount][2];
        kernel.pos = new int[amount];

        IntStream.range(0, amount)
        .parallel()
        .forEach(p -> {
            kernel.data[p][0] = Math.random();
            kernel.data[p][1] = Math.random();
        });

        kernel.x = Math.random();
        kernel.y = Math.random();
	}

	public void compute(final OpenCLDevice device, final EXECUTION_MODE mode) {
		if (device != null)
			logger.info("Execute on {} {} {}", device.getShortDescription(),					device.getName(), mode);
		else
		    logger.info("Execute on {}",mode);

		logger.info("Sample size {}",amount);
		
		kernel.setExecutionMode(mode);

		long t = System.currentTimeMillis();
		
		logger.info("before {}", System.currentTimeMillis() - t);
		long tmap = System.currentTimeMillis();

		kernel.operType = 0;
		kernel.execute(Range.create(device, amount));

		tmap=System.currentTimeMillis() - tmap;
		logger.info("after map {}", tmap);
		long treduce = System.currentTimeMillis();

		kernel.operType = 1;

		kernel.divider = device == null ? 256 : device.getMaxWorkGroupSize();

		kernel.size = amount;

		if (kernel.size > kernel.divider) {
			kernel.execute(Range.create(device, kernel.divider));
			kernel.size = kernel.divider;
		}

		kernel.divider = 1;

		kernel.execute(Range.create(device, kernel.divider));

		treduce=System.currentTimeMillis() - treduce;
		logger.info("after reduce {}", treduce);
		t = System.currentTimeMillis();

		logger.info("STAT {} {} sample {} map time {} reduce time {}",mode,device!=null?device.getName():"JTP", amount,tmap,treduce);
		
		logger.info("nearest point for {};{} is {};{} ({})", kernel.x, kernel.y, kernel.data[kernel.pos[0]][0], kernel.data[kernel.pos[0]][1], kernel.res[kernel.pos[0]][0]);

		kernel.dispose();
	}


	public static class KMapReducer extends Kernel {

		public int[] pos;

		public double[][] data;
		public double[][] res;
		int operType;
		int size;
		int divider;

		double x;
		double y;

		@Override
		public void run() {
			if (operType == 0)
				map();
			else if (operType == 1)
				reduce();
		}

		public void map() {
			final int gid = getGlobalId();
			pos[gid] = gid;

			res[gid][0] = (data[gid][0] - x) * (data[gid][0] - x) + (data[gid][1] - y) * (data[gid][1] - y);
		}

		private void reduce() {
			final int gid = getGlobalId();

			final int shift = gid;
			final int cnt = size / divider;

			for (int i = 1; i < cnt; i++)
				if (res[pos[shift]][0] > res[pos[shift + i * divider]][0])
					pos[shift] = pos[shift + i * divider];

			if (gid + 1 == divider && size % divider != 0)
				for (int i = 0; i < size % divider; i++)
					if (res[pos[shift]][0] > res[pos[cnt * divider + i]][0])
						pos[shift] = pos[cnt * divider + i];

		}
	}


}
