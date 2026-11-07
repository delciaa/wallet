package io.github.delciaa.wallet.transfer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

	Optional<Transfer> findByInitiatedByAndIdempotencyKey(UUID initiatedBy, String idempotencyKey);

	long countByInitiatedBy(UUID initiatedBy);

}
