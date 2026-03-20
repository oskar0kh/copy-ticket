package copy_ticket.copy_ticket.config.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class KstDateTimeUtils {

    public static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private KstDateTimeUtils() {
    }

    public static Instant nowInstant() {
        return ZonedDateTime.now(KST_ZONE_ID).toInstant();
    }

    public static LocalDateTime toKstLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(KST_ZONE_ID).toLocalDateTime();
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(KST_ZONE_ID).toInstant();
    }
}