package org.korolev.dens.blps_lab4_standalone.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleTopic {

    private Integer id;
    private String title;
    private String text;
    private String owner;

}
