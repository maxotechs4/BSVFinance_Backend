-- INSERT IGNORE INTO admin (username, password, full_name, email, role, enabled, created_at)
-- VALUES (
--     'staff',
--     '$2b$10$2kiOB60lmCWHF3HDIf/v8O.GhShrJvMmUtuk64Ev7bPyqC86gOnY2',
--     'Staff User',
--     'staff@microfinance.local',
--     'STAFF',
--     true,
--     NOW()
-- );

-- INSERT IGNORE INTO admin (username, password, full_name, email, role, enabled, created_at)
-- VALUES (
--     'admin',
--     '$2b$10$OymHP7QwgqMH5Y80H9HimeCDecFLMFGzbQS/sYP4RRPNScPjBPWO6',
--     'System Administrator',
--     'admin@microfinance.local',
--     'ADMIN',
--     true,
--     NOW()
-- );


INSERT INTO admin (username, password, full_name, email, role, enabled, created_at)
VALUES (
    'staff',
    '$2b$10$2kiOB60lmCWHF3HDIf/v8O.GhShrJvMmUtuk64Ev7bPyqC86gOnY2',
    'Staff User',
    'staff@microfinance.local',
    'STAFF',
    true,
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO admin (username, password, full_name, email, role, enabled, created_at)
VALUES (
    'admin',
    '$2b$10$OymHP7QwgqMH5Y80H9HimeCDecFLMFGzbQS/sYP4RRPNScPjBPWO6',
    'System Administrator',
    'admin@microfinance.local',
    'ADMIN',
    true,
    NOW()
) ON CONFLICT (username) DO NOTHING;

-- View-only login: can see every page but every create/edit/delete request is
-- rejected server-side (see SecurityConfig). Username: viewer / Password: viewer123
INSERT INTO admin (username, password, full_name, email, role, enabled, created_at)
VALUES (
    'viewer',
    '$2b$10$WMsnUiJwxAuZUNl6EEr/vea2UBWHTNfPz21ctL8O5ASrZPHwm.a2a',
    'Viewer Staff',
    'viewer@microfinance.local',
    'VIEWER',
    true,
    NOW()
) ON CONFLICT (username) DO NOTHING;