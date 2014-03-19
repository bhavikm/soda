import java.util.Comparator;

// Takes string of form "docid,value", extracts the value part to use for comparison
public class DoubleInStringComparator implements Comparator<String>
{
    @Override
    public int compare(String x, String y)
    {
		String[] partsOfX = x.split(",");
		String[] partsOfY = y.split(",");
		double xVal = Double.parseDouble(partsOfX[1]);
		double yVal = Double.parseDouble(partsOfY[1]);
		 
        if (xVal < yVal)
        {
            return 1;
        }
        if (xVal > yVal)
        {
            return -1;
        }
        return 0;
    }
}