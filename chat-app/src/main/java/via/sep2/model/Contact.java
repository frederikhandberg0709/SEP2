package via.sep2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact implements Serializable {
    private int id;
    private String username;
    private String firstName;
    private String lastName;
}
