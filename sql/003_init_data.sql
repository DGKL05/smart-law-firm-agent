USE smart_law_firm_agent;

INSERT INTO sys_role (id, role_code, role_name, status) VALUES
(1, 'ADMIN', '管理员', 1),
(2, 'USER', '普通用户', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

-- BCrypt 明文均为 123456
INSERT INTO sys_user (id, username, password, nickname, phone, email, status, deleted) VALUES
(1, 'admin', '$2a$10$CwTycUXWue0Thq9StjUM0uJ8/cu2hTz0xix9c5lxvkcS2u.FCBotW', '管理员', '13800000000', 'admin@example.com', 1, 0),
(2, 'user', '$2a$10$CwTycUXWue0Thq9StjUM0uJ8/cu2hTz0xix9c5lxvkcS2u.FCBotW', '普通用户', '13900000000', 'user@example.com', 1, 0)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status), deleted = 0;

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, permission, icon, sort_order, visible) VALUES
(1, 0, '首页', '/', 'Home', 'public:view', 'House', 1, 1),
(2, 0, '律所列表', '/law-firms', 'LawFirms', 'public:lawfirm:view', 'OfficeBuilding', 2, 1),
(3, 0, '律师列表', '/lawyers', 'Lawyers', 'public:lawyer:view', 'User', 3, 1),
(10, 0, '用户中心', '/user', 'UserLayout', 'user:view', 'Folder', 10, 1),
(20, 0, '管理后台', '/admin', 'AdminLayout', 'admin:view', 'Setting', 20, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 10), (1, 20),
(2, 1), (2, 2), (2, 3), (2, 10)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

INSERT INTO law_firm (id, name, province_code, province_name, city, address, phone, email, description, specialties, license_no, status, deleted) VALUES
(1, '华夏明德律师事务所', '110000', '北京市', '北京', '北京市朝阳区法务中心 18 层', '010-88886666', 'contact@mingde-law.example', '综合型律师事务所，提供民商事、公司与金融法律服务。', '民事民法,公司企业,经济金融', 'LAW-BJ-2026-001', 1, 0),
(2, '海岳知行律师事务所', '310000', '上海市', '上海', '上海市浦东新区陆家嘴法务大厦 9 层', '021-66668888', 'service@zhixing-law.example', '专注公司合规、涉外纠纷与金融争议解决。', '涉外纠纷,公司企业,经济金融', 'LAW-SH-2026-002', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

INSERT INTO lawyer (id, law_firm_id, name, gender, phone, email, category, title, experience_years, description, good_at, status, deleted) VALUES
(1, 1, '张明', '男', '13810000001', 'zhangming@example.com', '民事民法', '高级合伙人', 12, '长期处理婚姻家事、侵权与合同纠纷。', '合同纠纷、婚姻家事、侵权责任', 1, 0),
(2, 1, '李清', '女', '13810000002', 'liqing@example.com', '公司企业', '合伙人律师', 9, '服务创业公司、制造企业和互联网企业。', '股权设计、劳动合规、公司治理', 1, 0),
(3, 2, '王远', '男', '13810000003', 'wangyuan@example.com', '涉外纠纷', '资深律师', 10, '具备涉外合同与国际商事争议处理经验。', '涉外合同、跨境争议、仲裁', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), category = VALUES(category), status = 1, deleted = 0;
