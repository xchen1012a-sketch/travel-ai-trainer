-- =====================================================================
-- 演示数据注入脚本 (travel-ai-trainer)
-- 用法: mysql -u root -p123456 < database/seed_demo.sql
-- 所有账号密码均为: 123456
-- =====================================================================

USE teach_agent;

SET FOREIGN_KEY_CHECKS = 0;

-- 清理旧演示数据（按依赖反向清理）
DELETE FROM feedback WHERE conversation_id IN (SELECT id FROM (SELECT id FROM conversations WHERE title LIKE 'DEMO-%') c);
DELETE FROM messages WHERE conversation_id IN (SELECT id FROM (SELECT id FROM conversations WHERE title LIKE 'DEMO-%') c);
DELETE FROM task_submissions WHERE task_id IN (SELECT id FROM (SELECT id FROM interaction_tasks WHERE title LIKE 'DEMO-%') t);
DELETE FROM interaction_tasks WHERE title LIKE 'DEMO-%';
DELETE FROM conversations WHERE title LIKE 'DEMO-%';
DELETE FROM class_members WHERE class_id IN (SELECT id FROM (SELECT id FROM classes WHERE name LIKE 'DEMO-%') c);
DELETE FROM classes WHERE name LIKE 'DEMO-%';
DELETE FROM agents WHERE name LIKE 'DEMO-%';
DELETE FROM knowledge_files WHERE knowledge_base_id IN (SELECT id FROM (SELECT id FROM knowledge_bases WHERE name LIKE 'DEMO-%') k);
DELETE FROM knowledge_bases WHERE name LIKE 'DEMO-%';
DELETE FROM courses WHERE name LIKE 'DEMO-%';
DELETE FROM users WHERE username LIKE 'demo_%';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 1. 用户：1 教师 + 6 学生
-- =====================================================================
INSERT INTO users (role, username, password_hash, display_name, email, phone, status) VALUES
('TEACHER', 'demo_teacher', '{noop}123456', '王老师', 'teacher@demo.com', '13800000001', 'ACTIVE'),
('STUDENT', 'demo_stu01',  '{noop}123456', '李明',   'stu01@demo.com',   '13800000011', 'ACTIVE'),
('STUDENT', 'demo_stu02',  '{noop}123456', '张婷',   'stu02@demo.com',   '13800000012', 'ACTIVE'),
('STUDENT', 'demo_stu03',  '{noop}123456', '陈思雨', 'stu03@demo.com',   '13800000013', 'ACTIVE'),
('STUDENT', 'demo_stu04',  '{noop}123456', '刘子轩', 'stu04@demo.com',   '13800000014', 'ACTIVE'),
('STUDENT', 'demo_stu05',  '{noop}123456', '赵雨涵', 'stu05@demo.com',   '13800000015', 'ACTIVE'),
('STUDENT', 'demo_stu06',  '{noop}123456', '孙浩然', 'stu06@demo.com',   '13800000016', 'ACTIVE');

SET @tid       = (SELECT id FROM users WHERE username='demo_teacher');
SET @stu1      = (SELECT id FROM users WHERE username='demo_stu01');
SET @stu2      = (SELECT id FROM users WHERE username='demo_stu02');
SET @stu3      = (SELECT id FROM users WHERE username='demo_stu03');
SET @stu4      = (SELECT id FROM users WHERE username='demo_stu04');
SET @stu5      = (SELECT id FROM users WHERE username='demo_stu05');
SET @stu6      = (SELECT id FROM users WHERE username='demo_stu06');

-- =====================================================================
-- 2. 课程
-- =====================================================================
INSERT INTO courses (teacher_id, name, subject, grade_level, description, status) VALUES
(@tid, 'DEMO-金牌导游讲解实训', '旅游服务', '中级班', '以华东黄金线路为案例，训练讲解、应答与销售技巧。', 'PUBLISHED'),
(@tid, 'DEMO-定制游销售陪练',   '旅游销售', '初级班', '面向新销售顾问的客户沟通与方案设计能力训练。', 'PUBLISHED');

SET @course1 = (SELECT id FROM courses WHERE name='DEMO-金牌导游讲解实训');
SET @course2 = (SELECT id FROM courses WHERE name='DEMO-定制游销售陪练');

-- =====================================================================
-- 3. 智能体（agents）
-- =====================================================================
INSERT INTO agents (teacher_id, course_id, name, description, opening_message, system_prompt, status) VALUES
(@tid, @course1, 'DEMO-导游讲解陪练官', '模拟资深游客提问，针对景点讲解给出实时反馈。',
 '你好，我是今天的"挑剔游客"，请开始你的西湖白堤讲解吧～',
 '你扮演一位见多识广、提问犀利的游客。请基于学员讲解，从准确性/吸引力/互动性三个维度给出反馈与得分。', 'PUBLISHED'),
(@tid, @course2, 'DEMO-高端客户角色扮演',  '模拟年收入百万、追求深度体验的中年客户。',
 '我想给爸妈安排一次10天的日本深度游，预算30万，你能给我一些方案么？',
 '你是一位高净值客户，挑剔但讲道理。请用真实客户口吻提问、追问预算/行程细节，并对销售方案打分。', 'PUBLISHED');

SET @agent1 = (SELECT id FROM agents WHERE name='DEMO-导游讲解陪练官');
SET @agent2 = (SELECT id FROM agents WHERE name='DEMO-高端客户角色扮演');

-- =====================================================================
-- 4. 知识库
-- =====================================================================
INSERT INTO knowledge_bases (teacher_id, course_id, agent_id, name, description, status) VALUES
(@tid, @course1, @agent1, 'DEMO-华东景点讲解资料库', '收录西湖、乌镇、苏州园林等讲解词与历史背景。', 'ACTIVE'),
(@tid, @course2, @agent2, 'DEMO-高端定制产品库',     '日本/欧洲/极地高端线路产品资料与报价单。', 'ACTIVE');

SET @kb1 = (SELECT id FROM knowledge_bases WHERE name='DEMO-华东景点讲解资料库');
SET @kb2 = (SELECT id FROM knowledge_bases WHERE name='DEMO-高端定制产品库');

INSERT INTO knowledge_files (knowledge_base_id, uploader_id, original_name, storage_key, mime_type, file_size, parse_status) VALUES
(@kb1, @tid, '西湖十景讲解词.docx',       'demo/xihu.docx',     'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 28456, 'COMPLETED'),
(@kb1, @tid, '乌镇历史背景.pdf',          'demo/wuzhen.pdf',    'application/pdf', 102348, 'COMPLETED'),
(@kb1, @tid, '苏州园林文化解读.md',       'demo/suzhou.md',     'text/markdown', 8732, 'COMPLETED'),
(@kb2, @tid, '日本深度游产品手册.pdf',    'demo/japan.pdf',     'application/pdf', 245983, 'COMPLETED'),
(@kb2, @tid, '北欧极光线路报价.xlsx',     'demo/aurora.xlsx',   'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 35621, 'COMPLETED');

-- =====================================================================
-- 5. 班级 + 学生成员
-- =====================================================================
INSERT INTO classes (teacher_id, course_id, agent_id, name, class_code, description, join_enabled, status) VALUES
(@tid, @course1, @agent1, 'DEMO-2026春季导游A班', 'DEMO01', '面向新入职导游的强化训练班。', 1, 'ACTIVE'),
(@tid, @course2, @agent2, 'DEMO-高端销售集训营',  'DEMO02', '面向资深销售的高端客户陪练班。', 1, 'ACTIVE');

SET @class1 = (SELECT id FROM classes WHERE class_code='DEMO01');
SET @class2 = (SELECT id FROM classes WHERE class_code='DEMO02');

INSERT INTO class_members (class_id, student_id, nickname, status) VALUES
(@class1, @stu1, '李明',   'ACTIVE'),
(@class1, @stu2, '张婷',   'ACTIVE'),
(@class1, @stu3, '陈思雨', 'ACTIVE'),
(@class1, @stu4, '刘子轩', 'ACTIVE'),
(@class2, @stu3, '陈思雨', 'ACTIVE'),
(@class2, @stu5, '赵雨涵', 'ACTIVE'),
(@class2, @stu6, '孙浩然', 'ACTIVE');

-- =====================================================================
-- 6. 互动任务
-- =====================================================================
INSERT INTO interaction_tasks (teacher_id, class_id, agent_id, title, instruction, due_at, status) VALUES
(@tid, @class1, @agent1, 'DEMO-西湖白堤现场讲解',
 '请以"白堤"为主题，进行3分钟左右的导游讲解，并应对陪练官随机提问。', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PUBLISHED'),
(@tid, @class2, @agent2, 'DEMO-高净值客户首次接触',
 '客户预算30万，要为父母定制10天日本深度游。请完成首次需求挖掘并给出初步方案。', DATE_ADD(NOW(), INTERVAL 5 DAY), 'PUBLISHED');

SET @task1 = (SELECT id FROM interaction_tasks WHERE title='DEMO-西湖白堤现场讲解');
SET @task2 = (SELECT id FROM interaction_tasks WHERE title='DEMO-高净值客户首次接触');

-- =====================================================================
-- 7. 对话 + 消息（演讲展示的核心）
-- =====================================================================

-- 对话 A：李明 vs 导游陪练官（已完成多轮，最有看点）
INSERT INTO conversations (class_id, agent_id, student_id, title, status, last_message_at)
VALUES (@class1, @agent1, @stu1, 'DEMO-李明·西湖白堤讲解陪练', 'OPEN', NOW());
SET @conv1 = LAST_INSERT_ID();

INSERT INTO messages (conversation_id, sender_type, sender_id, content, content_type, token_count, latency_ms, created_at) VALUES
(@conv1, 'AGENT',   NULL,  '你好，我是今天的"挑剔游客"，请开始你的西湖白堤讲解吧～', 'MARKDOWN', 45, 320, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(@conv1, 'STUDENT', @stu1, '各位团友大家好，欢迎来到杭州西湖。我们脚下这条堤叫白堤，全长约1公里，得名于唐代大诗人白居易……', 'TEXT', 128, NULL, DATE_SUB(NOW(), INTERVAL 29 MINUTE)),
(@conv1, 'AGENT',   NULL,  '等等，导游，我看资料说白堤其实跟白居易没直接关系，是后人为了纪念才改名的，你怎么解释？', 'TEXT', 78, 410, DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(@conv1, 'STUDENT', @stu1, '您说得很专业！白堤原名"白沙堤"，唐代时白居易任杭州刺史期间确实修筑过一条堤——但那条在如今的少年宫附近，叫"白公堤"。现在的白堤是后人为纪念白居易将"白沙堤"改称白堤的。', 'TEXT', 156, NULL, DATE_SUB(NOW(), INTERVAL 27 MINUTE)),
(@conv1, 'AGENT',   NULL,  '👍 这次回答准确多了。继续：白堤上的"断桥残雪"为什么叫"断桥"？请用1分钟讲清楚，要带点故事。', 'MARKDOWN', 65, 380, DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
(@conv1, 'STUDENT', @stu1, '断桥并不是真的断了，而是因为冬日雪后桥的阳面雪化、阴面留白，从远处看就像桥断了一截，所以叫"断桥残雪"。当然，让它真正出名的，是许仙和白娘子的故事——他们就是在这里"一伞定情"。', 'TEXT', 142, NULL, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(@conv1, 'AGENT',   NULL,  '本轮反馈：\n- 准确性：4.5 / 5\n- 吸引力：4.0 / 5\n- 互动性：3.5 / 5\n\n建议：开篇可以加一个钩子提问（比如"大家知道白堤为什么不叫白居易堤吗？"）来抓住游客注意力。', 'MARKDOWN', 96, 520, DATE_SUB(NOW(), INTERVAL 24 MINUTE));

-- 对话 B：张婷 vs 导游陪练官（进行中）
INSERT INTO conversations (class_id, agent_id, student_id, title, status, last_message_at)
VALUES (@class1, @agent1, @stu2, 'DEMO-张婷·乌镇水乡讲解陪练', 'OPEN', NOW());
SET @conv2 = LAST_INSERT_ID();

INSERT INTO messages (conversation_id, sender_type, sender_id, content, content_type, token_count, latency_ms, created_at) VALUES
(@conv2, 'AGENT',   NULL,  '欢迎，今天讲乌镇。我先抛个问题：你能用一句话讲清楚乌镇东栅和西栅的区别吗？', 'TEXT', 52, 290, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(@conv2, 'STUDENT', @stu2, '一句话：东栅是原住民的生活乌镇，白天看；西栅是景区化的夜游乌镇，晚上美。', 'TEXT', 48, NULL, DATE_SUB(NOW(), INTERVAL 17 MINUTE)),
(@conv2, 'AGENT',   NULL,  '👏 干净利落，给5分。接下来请讲乌镇为什么被称为"中国最后的枕水人家"。', 'MARKDOWN', 60, 350, DATE_SUB(NOW(), INTERVAL 16 MINUTE));

-- 对话 C：陈思雨 vs 高端客户陪练（销售场景）
INSERT INTO conversations (class_id, agent_id, student_id, title, status, last_message_at)
VALUES (@class2, @agent2, @stu3, 'DEMO-陈思雨·日本深度游销售陪练', 'OPEN', NOW());
SET @conv3 = LAST_INSERT_ID();

INSERT INTO messages (conversation_id, sender_type, sender_id, content, content_type, token_count, latency_ms, created_at) VALUES
(@conv3, 'AGENT',   NULL,  '我想给爸妈安排一次10天的日本深度游，预算30万，你能给我一些方案么？我父母都65岁左右，腿脚还行，不想跟那种购物团。', 'TEXT', 88, 380, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(@conv3, 'STUDENT', @stu3, '先生您好，30万10天的预算非常充裕，可以做到很深度的体验。在出方案前我想先确认几点：\n1. 出行月份大概是什么时候？\n2. 父母对温泉、和服、茶道这类传统体验感兴趣吗？\n3. 有没有特别想去但又怕累的目的地，比如富士山周边或北海道？', 'MARKDOWN', 132, NULL, DATE_SUB(NOW(), INTERVAL 44 MINUTE)),
(@conv3, 'AGENT',   NULL,  '不错，没急着报方案——这点比上一位顾问强。\n1. 计划11月去，看红叶；2. 都想体验；3. 北海道我们去过，这次想京都+东京线，慢一点。', 'TEXT', 92, 410, DATE_SUB(NOW(), INTERVAL 43 MINUTE)),
(@conv3, 'STUDENT', @stu3, '太好了，11月正是关西红叶季。我初步给您一个"京都6天 + 箱根2天 + 东京2天"的节奏：\n- 京都：京都岚山红叶专列 + 醍醐寺红叶夜赏 + 茶道+和服体验1日\n- 箱根：一泊二食温泉怀石（推荐强罗花坛）\n- 东京：自由活动+米其林餐厅2席\n\n这个版本人均预算约13万，双人26万，含直飞商务舱、酒店均为5星或老铺日式旅馆，全程中文领队随行。您看节奏可以吗？', 'MARKDOWN', 245, NULL, DATE_SUB(NOW(), INTERVAL 42 MINUTE)),
(@conv3, 'AGENT',   NULL,  '节奏可以，但商务舱不一定要，能不能换成头等舱单程？另外强罗花坛我听说过，环境怎么样？', 'TEXT', 68, 360, DATE_SUB(NOW(), INTERVAL 41 MINUTE));

-- 对话 D：刘子轩（已收到反馈）
INSERT INTO conversations (class_id, agent_id, student_id, title, status, last_message_at)
VALUES (@class1, @agent1, @stu4, 'DEMO-刘子轩·苏州园林讲解陪练', 'CLOSED', DATE_SUB(NOW(), INTERVAL 2 DAY));
SET @conv4 = LAST_INSERT_ID();

INSERT INTO messages (conversation_id, sender_type, sender_id, content, content_type, created_at) VALUES
(@conv4, 'AGENT',   NULL,  '今天讲拙政园，请开始。', 'TEXT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@conv4, 'STUDENT', @stu4, '拙政园是苏州四大名园之首，始建于明代正德年间……（讲解略）', 'TEXT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@conv4, 'AGENT',   NULL,  '本次综合得分 4.2 / 5。亮点：历史脉络梳理清晰；待改进：互动性偏弱、缺少现场指引语。', 'MARKDOWN', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- =====================================================================
-- 8. 任务提交（关联对话）
-- =====================================================================
INSERT INTO task_submissions (task_id, student_id, conversation_id, status, submitted_at, teacher_comment) VALUES
(@task1, @stu1, @conv1, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL),
(@task1, @stu2, @conv2, 'IN_PROGRESS', NULL, NULL),
(@task1, @stu4, @conv4, 'REVIEWED', DATE_SUB(NOW(), INTERVAL 2 DAY), '历史背景扎实，下次注意现场互动节奏。'),
(@task2, @stu3, @conv3, 'IN_PROGRESS', NULL, NULL);

-- =====================================================================
-- 9. 学员对AI的反馈（演示反馈面板）
-- =====================================================================
INSERT INTO feedback (conversation_id, message_id, user_id, rating, content) VALUES
(@conv1, NULL, @stu1, 5, 'AI 陪练问题非常贴合真实游客，逼着我把白堤的历史细节翻出来了。'),
(@conv3, NULL, @stu3, 4, '客户角色还原很真实，希望能多一点情感化的刁难。'),
(@conv4, NULL, @stu4, 4, '复盘评分很客观，建议增加录音回放。');

-- =====================================================================
-- 完成
-- =====================================================================
SELECT '✅ 演示数据注入完成' AS msg;
SELECT
  (SELECT COUNT(*) FROM users WHERE username LIKE 'demo_%')        AS users_cnt,
  (SELECT COUNT(*) FROM courses WHERE name LIKE 'DEMO-%')          AS courses_cnt,
  (SELECT COUNT(*) FROM agents  WHERE name LIKE 'DEMO-%')          AS agents_cnt,
  (SELECT COUNT(*) FROM classes WHERE name LIKE 'DEMO-%')          AS classes_cnt,
  (SELECT COUNT(*) FROM conversations WHERE title LIKE 'DEMO-%')   AS conversations_cnt,
  (SELECT COUNT(*) FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE title LIKE 'DEMO-%')) AS messages_cnt;

SELECT '账号: demo_teacher / demo_stu01~06   密码: 123456   班级码: DEMO01 / DEMO02' AS tips;
