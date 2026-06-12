package LNASC.REGINOTES.Repositories;

import LNASC.REGINOTES.Models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = "SELECT n FROM Notification n WHERE n.notificationOwner.id = :userId AND n.id = :notificationId")
    Optional<Notification> findByUserIdAndId(@Param("userId") UUID userId, @Param("notificationId") UUID notificationId);

    @Query("SELECT n FROM Notification n WHERE n.read = false AND n.notificationOwner.id =:userId ")
    List<Notification> findByNotReadAndUserId(@Param("userId") UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.notificationOwner.id =:userId ")
    List<Notification> findByUserId(@Param("userId") UUID userId);
}
