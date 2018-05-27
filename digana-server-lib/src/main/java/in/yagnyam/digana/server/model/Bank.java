package in.yagnyam.digana.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import lombok.*;

@Cache
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "bankId")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bank {

    @Id
    private String bankNumber;

    private String name;

    private String accountAuthorizationApiEndpoint;

    private String transferApiEndpoint;

}
