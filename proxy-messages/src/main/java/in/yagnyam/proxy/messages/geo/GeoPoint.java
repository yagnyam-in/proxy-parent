package in.yagnyam.proxy.messages.geo;

import in.yagnyam.proxy.ProxyBaseObject;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class GeoPoint implements ProxyBaseObject {

    @NonNull
    private Double latitude;

    @NonNull
    private Double longitude;

    @Override
    public boolean isValid() {
        return latitude != null && longitude != null;
    }
}
