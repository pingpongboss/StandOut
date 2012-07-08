package wei.mark.standout;

public class Utils {
	public static boolean isSet(long flags, long flag) {
		return (flags & flag) == flag;
	}
}
