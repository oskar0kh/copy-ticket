package copy_ticket.copy_ticket.config.time;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

@Converter(autoApply = true)
public class KstInstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        LocalDateTime kstDateTime = KstDateTimeUtils.toKstLocalDateTime(attribute);
        return kstDateTime == null ? null : Timestamp.valueOf(kstDateTime);
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        return dbData == null ? null : KstDateTimeUtils.toInstant(dbData.toLocalDateTime());
    }
}