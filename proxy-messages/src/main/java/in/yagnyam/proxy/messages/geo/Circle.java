package in.yagnyam.proxy.messages.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Circle implements ProxyBaseObject {

    @NonNull
    private String circleId;

    @NonNull
    private String color;

    @NonNull
    private String number;

    @JsonIgnore
    @Override
    public boolean isValid() {
        return StringUtils.isValid(circleId)
                && StringUtils.isValid(color)
                && StringUtils.isValid(number);
    }

}
