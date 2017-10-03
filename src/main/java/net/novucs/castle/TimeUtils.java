package net.novucs.castle;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

    private static final Comparator<TimeUnit> REVERSE_TIME_UNIT_COMPARATOR = (t1, t2) -> -t1.compareTo(t2);
    private static final SortedMap<TimeUnit, Word> LONG_TIME_UNITS = new TreeMap<>(REVERSE_TIME_UNIT_COMPARATOR);
    private static final SortedMap<TimeUnit, Word> SHORT_TIME_UNITS = new TreeMap<>(REVERSE_TIME_UNIT_COMPARATOR);

    static {
        LONG_TIME_UNITS.put(TimeUnit.DAYS, new Word(" Day", " Days"));
        LONG_TIME_UNITS.put(TimeUnit.HOURS, new Word(" Hour", " Hours"));
        LONG_TIME_UNITS.put(TimeUnit.MINUTES, new Word(" Minute", " Minutes"));
        LONG_TIME_UNITS.put(TimeUnit.SECONDS, new Word(" Second", " Seconds"));

        SHORT_TIME_UNITS.put(TimeUnit.DAYS, new Word("d", "d"));
        SHORT_TIME_UNITS.put(TimeUnit.HOURS, new Word("h", "h"));
        SHORT_TIME_UNITS.put(TimeUnit.MINUTES, new Word("m", "m"));
        SHORT_TIME_UNITS.put(TimeUnit.SECONDS, new Word("s", "s"));
    }

    private TimeUtils() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    /**
     * Formats the provided time in any style.
     *
     * @param unit      the {@link TimeUnit} the time is provided in.
     * @param time      the time.
     * @param units     each {@link TimeUnit} to include and their relevant text.
     * @param delimiter the delimiter to be used between each time unit or {@code null}.
     * @return the formatted time text.
     */
    public static String formatTime(TimeUnit unit, long time, SortedMap<TimeUnit, Word> units, String delimiter) {
        StringBuilder builder = new StringBuilder();
        time = units.lastKey().convert(time, unit);

        Iterator<Map.Entry<TimeUnit, Word>> it = units.entrySet().iterator();
        Map.Entry<TimeUnit, Word> entry;

        while (time > 0 && it.hasNext()) {
            entry = it.next();
            long total = entry.getKey().convert(time, units.lastKey());

            if (total == 0) {
                continue;
            }

            if (builder.length() > 0 && delimiter != null) {
                builder.append(delimiter);
            }

            builder.append(total);
            builder.append(total == 1 ? entry.getValue().getSingular() : entry.getValue().getPlural());
            time -= units.lastKey().convert(total, entry.getKey());
        }

        return builder.toString();
    }

    /**
     * Formats the provided time in a longhand style.
     *
     * @param unit the {@link TimeUnit} the time is provided in.
     * @param time the time.
     * @return the formatted time text.
     */
    public static String formatTimeLonghand(TimeUnit unit, long time) {
        return formatTime(unit, time, LONG_TIME_UNITS, " ");
    }

    /**
     * Formats the provided time in a shorthand style.
     *
     * @param unit the {@link TimeUnit} the time is provided in.
     * @param time the time.
     * @return the formatted time text.
     */
    public static String formatTimeShorthand(TimeUnit unit, long time) {
        return formatTime(unit, time, SHORT_TIME_UNITS, null);
    }

    /**
     * Formats the provided date to a human readable text.
     *
     * @param millis the UTC time in milliseconds.
     * @return the formatted date.
     */
    public static String formatDate(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        StringJoiner joiner = new StringJoiner("/");
        joiner.add(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
        joiner.add(Integer.toString(calendar.get(Calendar.MONTH) + 1));
        joiner.add(Integer.toString(calendar.get(Calendar.YEAR)));
        return joiner.toString();
    }

    /**
     * A word that can have both plural and singular form.
     */
    private static class Word {

        private final String singular;
        private final String plural;

        /**
         * Constructs a new {@link Word}.
         *
         * @param singular the singular form.
         * @param plural   the plural form.
         */
        public Word(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
        }

        /**
         * Gets the singular form of this word.
         *
         * @return the singular form.
         */
        public String getSingular() {
            return singular;
        }

        /**
         * Gets the plural form of this word.
         *
         * @return the plural form.
         */
        public String getPlural() {
            return plural;
        }
    }
}
