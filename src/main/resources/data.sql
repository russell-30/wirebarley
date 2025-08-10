-- 계정 데이터 추가
INSERT INTO accounts (account_number, balance, status, daily_withdraw_limit, daily_transfer_limit, created_at)
SELECT account_number, balance, status, daily_withdraw_limit, daily_transfer_limit, created_at
FROM (
         SELECT
             '1000000001' as account_number,
             1000000 as balance,
             'ACTIVE' as status,
             1000000 as daily_withdraw_limit,
             3000000 as daily_transfer_limit,
             NOW() as created_at
         UNION ALL
         SELECT
             '1000000002',
             500000,
             'ACTIVE',
             1000000,
             3000000,
             NOW()
         UNION ALL
         SELECT
             '1000000003',
             2000000,
             'ACTIVE',
             1000000,
             3000000,
             NOW()
     ) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE account_number IN ('1000000001', '1000000002', '1000000003')
);

-- 거래 내역 데이터 추가
INSERT INTO transactions (transaction_id, from_account_id, to_account_id, amount, fee, type, status, description, created_at)
SELECT transaction_id, from_account_id, to_account_id, amount, fee, type, status, description, created_at
FROM (
         SELECT
             'TRX0000001' as transaction_id,
             1 as from_account_id,
             2 as to_account_id,
             50000 as amount,
             500 as fee,
             'TRANSFER' as type,
             'COMPLETED' as status,
             '거래1' as description,
             NOW() as created_at
         UNION ALL
         SELECT
             'TRX0000002',
             2,
             3,
             30000,
             300,
             'TRANSFER',
             'COMPLETED',
             '거래2',
             NOW()
         UNION ALL
         SELECT
             'TRX0000003',
             1,
             NULL,
             20000,
             NULL,
             'TRANSFER',
             'COMPLETED',
             '출금1',
             NOW()
         UNION ALL
         SELECT
             'TRX0000004',
             NULL,
             3,
             100000,
             NULL,
             'DEPOSIT',
             'COMPLETED',
             '입금1',
             NOW()
     ) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE transaction_id IN ('TRX0000001', 'TRX0000002', 'TRX0000003', 'TRX0000004')
);