package org.example.botwebsocket.repo;

import org.example.botwebsocket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    User findByChatId(Long chatId);

}