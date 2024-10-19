package org.korolev.dens.ratingservice.responces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class StatsMessage {

    private Integer topicId;
    private String producer;
    private String producerAction;
    private String topicOwner;

}
