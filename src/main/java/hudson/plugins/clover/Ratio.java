package hudson.plugins.clover;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents <tt>x/y</tt> where x={@link #numerator} and y={@link #denominator}.
 */
final public class Ratio implements Serializable, CoverageBarProvider {
    
    public final float numerator;
    public final float denominator;

    public static final NumberFormat PC_ROUND_DOWN_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final NumberFormat PC_ROUND_UP_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        PC_ROUND_DOWN_FORMAT.setMaximumFractionDigits(1);
        PC_ROUND_DOWN_FORMAT.setRoundingMode(RoundingMode.DOWN);

        PC_ROUND_UP_FORMAT.setMaximumFractionDigits(1);
        PC_ROUND_UP_FORMAT.setRoundingMode(RoundingMode.UP);
    }

    private Ratio(float numerator, float denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Gets "x/y" representation.
     * @return String in "x/y" format
     */
    public String toString() {
        return print(numerator)+"/"+print(denominator);
    }

    private String print(float f) {
        int i = (int) f;
        if(i==f)
            return String.valueOf(i);
        else
            return String.valueOf(f);
    }

    /**
     * Gets the percentage in integer.
     * @return String percentage
     */
    public String getPercentage1d() {
        return PC_ROUND_DOWN_FORMAT.format(getPercentageFloat());
    }

    public String getPercentageStr() {
        return denominator > 0 ? PC_ROUND_DOWN_FORMAT.format(getPercentageFloat()) + "%" : "-";
    }


    private String pcFormat(float pc) {
        return PC_ROUND_DOWN_FORMAT.format(pc) + "%";
    }

    public String getPcWidth() {
        return pcFormat(getPercentageFloat());
    }


    public String getPcUncovered() {
        float pcUncovered = 100.0f - getPercentageFloat();

        return PC_ROUND_UP_FORMAT.format(pcUncovered) + "%";

    }

    public String getPcCovered() {
        return getPercentageStr();
    }

    public String getHasData() {
        return "" + (denominator > 0);
    }


    /**
     * Gets the percentage in integer.
     * @return int percentage
     */
    public int getPercentage() {
        /* intentional truncation to simulate floor() */
        return (int)getPercentageFloat();
    }

    /**
     * Gets the percentage in float.
     * @return float percentage
     */
    public float getPercentageFloat() {
        if (Float.compare(numerator, denominator) >= 0)
            return 100; // n >= d, even if d == 0
        if (Float.compare(denominator, 0.0f) == 0)
            return 0.0f; // any other case where d == 0
        return 100 * numerator / denominator;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ratio ratio = (Ratio) o;

        return Float.compare(ratio.denominator, denominator)==0
            && Float.compare(ratio.numerator, numerator)==0;

    }

    public int hashCode() {
        int result;
        result = numerator != +0.0f ? Float.floatToIntBits(numerator) : 0;
        result = 31 * result + denominator != +0.0f ? Float.floatToIntBits(denominator) : 0;
        return result;
    }

    private static final long serialVersionUID = 1L;

//
// fly-weight patterns for common Ratio instances (x/y) where x<y
// and x,y are integers.
//
    private static final Ratio[] COMMON_INSTANCES = new Ratio[256];

    /**
     * Creates a new instance of {@link Ratio}.
     * @param x nominator
     * @param y denominator
     * @return Ratio
     */
    public static Ratio create(float x, float y) {
        int xx= (int) x;
        int yy= (int) y;

        if(xx==x && yy==y) {
            int idx = yy * (yy + 1) / 2 + xx;
            if(0<=idx && idx<COMMON_INSTANCES.length) {
                Ratio r = COMMON_INSTANCES[idx];
                if(r==null)
                    COMMON_INSTANCES[idx] = r = new Ratio(x,y);
                return r;
            }
        }

        return new Ratio(x,y);
    }
}
