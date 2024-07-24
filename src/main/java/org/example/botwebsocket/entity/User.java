package org.example.botwebsocket.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.botwebsocket.entity.enums.UserStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long chatId;
    @JsonIgnore
    private Integer counter;
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    private UserStatus status;
    @JsonIgnore
    private Integer editMessageId;
    @JsonIgnore
    private Integer currentCategoryId;
    @JsonIgnore
    private Integer currentProductId;
    @JsonIgnore
    @OneToOne
    private Basket basket;
}
