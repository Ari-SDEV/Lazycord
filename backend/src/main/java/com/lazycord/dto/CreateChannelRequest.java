package com.lazycord.dto;

import com.lazycord.model.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequest {
    private String name;
    private String description;
    private Channel.ChannelType type;
}
