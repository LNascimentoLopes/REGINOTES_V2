package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.email =:email")
    Optional<User> findByEmail(@Param("email")String email);

    @Modifying
    @Query("DELETE User u WHERE u.id = :userId")
    void deactivateByUserId(@Param("UserId") UUID userId);

}
