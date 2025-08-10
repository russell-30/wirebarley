package com.example.account.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class LockUtil {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 분산 락을 사용하여 작업을 실행합니다.
     *
     * @param lockName 락의 이름 (예: "account-{accountNumber}")
     * @param action 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T runWithLock(String lockName, Supplier<T> action) {
        try {
            // SELECT ... FOR UPDATE를 사용하여 분산 락 획득
            entityManager.createNativeQuery("SELECT GET_LOCK(:lockName, 10)")
                    .setParameter("lockName", lockName)
                    .getSingleResult();

            // 실제 작업 수행
            return action.get();
        } finally {
            // 락 해제
            entityManager.createNativeQuery("SELECT RELEASE_LOCK(:lockName)")
                    .setParameter("lockName", lockName)
                    .getSingleResult();
        }
    }

    /**
     * 분산 락을 사용하여 반환값이 없는 작업을 실행합니다.
     *
     * @param lockName 락의 이름
     * @param action 실행할 작업
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runWithLock(String lockName, Runnable action) {
        runWithLock(lockName, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 계좌 번호에 대한 락을 획득하여 작업을 실행합니다.
     *
     * @param accountNumber 계좌 번호
     * @param action 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    public <T> T runWithAccountLock(String accountNumber, Supplier<T> action) {
        return runWithLock("account-" + accountNumber, action);
    }

    /**
     * 계좌 번호에 대한 락을 획득하여 반환값이 없는 작업을 실행합니다.
     *
     * @param accountNumber 계좌 번호
     * @param action 실행할 작업
     */
    public void runWithAccountLock(String accountNumber, Runnable action) {
        runWithLock("account-" + accountNumber, action);
    }


}