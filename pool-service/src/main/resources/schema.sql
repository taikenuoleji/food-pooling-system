-- ============================================
-- 拼单服务数据库初始化脚本 (H2兼容)
-- 基于 design.md 设计文档
-- ============================================

-- 商家表
CREATE TABLE IF NOT EXISTS merchants (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id     VARCHAR(20)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    description     VARCHAR(500),
    address         VARCHAR(200),
    image_url       VARCHAR(500),
    rating          DECIMAL(2,1)    DEFAULT 4.5,
    monthly_sales   INT             DEFAULT 0,
    delivery_time   VARCHAR(50),
    delivery_fee    BIGINT          DEFAULT 0,
    min_order       BIGINT          DEFAULT 0,
    packaging_fee   BIGINT          DEFAULT 0,
    promo           VARCHAR(200),
    status          VARCHAR(16)     DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_id UNIQUE (merchant_id)
);

-- 菜品表 (注意：代码中使用的是 menu_items 表名)
CREATE TABLE IF NOT EXISTS menu_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id         VARCHAR(20)     NOT NULL,
    merchant_id     VARCHAR(20)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    price           BIGINT          NOT NULL,
    image_url       VARCHAR(500),
    category        VARCHAR(50),
    status          VARCHAR(16)     DEFAULT 'ACTIVE',
    sort_order      INT             DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_item_id UNIQUE (item_id)
);

-- 拼单主表
CREATE TABLE IF NOT EXISTS pools (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pool_id             VARCHAR(20)     NOT NULL,
    invite_code         VARCHAR(8)      NOT NULL,
    creator_id          VARCHAR(20)     NOT NULL,
    merchant_id         VARCHAR(20)     NOT NULL,
    merchant_name       VARCHAR(100)    NOT NULL,
    status              VARCHAR(15)     NOT NULL DEFAULT 'CREATED',
    min_members         INT             NOT NULL DEFAULT 2,
    min_amount          BIGINT          NOT NULL DEFAULT 0,
    deadline_minutes    INT             NOT NULL DEFAULT 30,
    current_members     INT             NOT NULL DEFAULT 0,
    current_food_amount BIGINT          NOT NULL DEFAULT 0,
    order_id            VARCHAR(20),
    delivery_fee        BIGINT          DEFAULT 0,
    packaging_fee       BIGINT          DEFAULT 0,
    remark              VARCHAR(500),
    expires_at          TIMESTAMP       NOT NULL,
    formed_at           TIMESTAMP,
    dissolved_at        TIMESTAMP,
    dissolve_reason     VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pool_id UNIQUE (pool_id),
    CONSTRAINT uk_invite_code UNIQUE (invite_code)
);

-- 拼单参与者表
CREATE TABLE IF NOT EXISTS pool_participants (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    participant_id  VARCHAR(20)     NOT NULL,
    pool_id         VARCHAR(20)     NOT NULL,
    user_id         VARCHAR(20)     NOT NULL,
    role            VARCHAR(10)     NOT NULL DEFAULT 'MEMBER',
    address_id      VARCHAR(20),
    food_amount     BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    remark          VARCHAR(200),
    joined_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at         TIMESTAMP,
    CONSTRAINT uk_participant_id UNIQUE (participant_id)
);

-- 拼单菜品明细表
CREATE TABLE IF NOT EXISTS pool_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pool_item_id    VARCHAR(20)     NOT NULL,
    pool_id         VARCHAR(20)     NOT NULL,
    participant_id  VARCHAR(20)     NOT NULL,
    user_id         VARCHAR(20)     NOT NULL,
    item_id         VARCHAR(20)     NOT NULL,
    item_name       VARCHAR(100)    NOT NULL,
    unit_price      BIGINT          NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    total_price     BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pool_item_id UNIQUE (pool_item_id)
);

-- ============================================
-- 插入商家测试数据 (16个商家，完整数据)
-- ============================================
INSERT INTO merchants (merchant_id, name, category, description, address, image_url, rating, monthly_sales, delivery_time, delivery_fee, min_order, packaging_fee, promo, status) VALUES
('MER0000000001', '茶颜悦色', '奶茶甜品', '长沙网红奶茶品牌，主打新中式茶饮', '北京市朝阳区三里屯路19号', 'images/M001.png', 4.8, 5200, '25-35分钟', 300, 1500, 200, '满30减5', 'ACTIVE'),
('MER0000000002', '麦当劳', '快餐简餐', '全球知名快餐连锁品牌', '北京市朝阳区建国路88号', 'images/M002.png', 4.5, 8500, '20-30分钟', 500, 2000, 300, '新人首单立减10元', 'ACTIVE'),
('MER0000000003', '海底捞火锅', '火锅', '知名火锅连锁品牌，服务至上', '北京市朝阳区望京SOHO', 'images/M003.png', 4.9, 3200, '40-55分钟', 800, 5000, 500, NULL, 'ACTIVE'),
('MER0000000004', '外婆家', '中餐', '知名杭帮菜连锁品牌', '北京市朝阳区大望路', 'images/M004.png', 4.6, 4100, '30-45分钟', 600, 3000, 400, '满50减8', 'ACTIVE'),
('MER0000000005', '必胜客', '西餐', '知名披萨连锁品牌', '北京市朝阳区国贸商城', 'images/M005.png', 4.4, 6300, '35-50分钟', 500, 2500, 300, '周二全场8折', 'ACTIVE'),
('MER0000000006', '肯德基', '快餐简餐', '全球著名炸鸡快餐品牌', '北京市海淀区中关村大街1号', 'images/M006.png', 4.3, 9200, '20-30分钟', 400, 1800, 300, '疯狂星期四特惠', 'ACTIVE'),
('MER0000000007', '喜茶', '奶茶甜品', '新式茶饮开创者', '北京市朝阳区三里屯太古里', 'images/M007.png', 4.7, 4800, '20-30分钟', 300, 1800, 200, '买二送一', 'ACTIVE'),
('MER0000000008', '瑞幸咖啡', '咖啡饮品', '中国领先的咖啡品牌', '北京市朝阳区望京街道', 'images/M008.png', 4.5, 7100, '15-25分钟', 200, 1200, 100, '首杯9.9元', 'ACTIVE'),
('MER0000000009', '黄焖鸡米饭', '中餐', '经典中式快餐', '北京市海淀区五道口', 'images/M009.png', 4.2, 5600, '25-35分钟', 400, 1500, 200, '加饭免费', 'ACTIVE'),
('MER0000000010', '兰州拉面', '面食', '正宗兰州牛肉拉面', '北京市西城区西单', 'images/M010.png', 4.4, 4500, '20-30分钟', 300, 1200, 200, NULL, 'ACTIVE'),
('MER0000000011', '呷哺呷哺', '火锅', '时尚小火锅品牌', '北京市朝阳区CBD', 'images/M011.png', 4.5, 3800, '35-45分钟', 600, 3500, 400, '套餐优惠', 'ACTIVE'),
('MER0000000012', '真功夫', '中餐', '蒸品中式快餐', '北京市东城区王府井', 'images/M012.png', 4.3, 3200, '25-35分钟', 500, 2000, 300, '满40减6', 'ACTIVE'),
('MER0000000013', '赛百味', '西餐', '健康三明治品牌', '北京市朝阳区国贸', 'images/M013.png', 4.2, 2800, '20-30分钟', 400, 2000, 200, '买一送一', 'ACTIVE'),
('MER0000000014', '吉野家', '快餐简餐', '日式牛肉饭连锁', '北京市海淀区万柳', 'images/M014.png', 4.4, 4200, '20-30分钟', 400, 1800, 200, '加量不加价', 'ACTIVE'),
('MER0000000015', '张亮麻辣烫', '麻辣烫', '知名麻辣烫连锁品牌', '北京市朝阳区双井', 'images/M015.png', 4.3, 5100, '25-35分钟', 300, 1500, 200, '满20减3', 'ACTIVE'),
('MER0000000016', 'CoCo都可', '奶茶甜品', '知名奶茶连锁品牌', '北京市西城区金融街', 'images/M016.png', 4.5, 4600, '20-30分钟', 300, 1200, 200, '第二杯半价', 'ACTIVE');

-- ============================================
-- 插入菜品测试数据 (每个商家4个菜品，共64个菜品)
-- ============================================
INSERT INTO menu_items (item_id, merchant_id, name, description, price, category, status, sort_order) VALUES
-- 茶颜悦色 (4个)
('ITM0000000001', 'MER0000000001', '幽兰拿铁', '招牌奶茶，奶油顶加碧根果碎', 1800, '招牌', 'AVAILABLE', 1),
('ITM0000000002', 'MER0000000001', '声声乌龙', '乌龙茶底，清香怡人', 1500, '茶饮', 'AVAILABLE', 2),
('ITM0000000003', 'MER0000000001', '桂花弄', '桂花乌龙奶茶', 1600, '茶饮', 'AVAILABLE', 3),
('ITM0000000004', 'MER0000000001', '筝筝纸鸢', '茉莉绿茶奶茶', 1700, '茶饮', 'AVAILABLE', 4),
-- 麦当劳 (4个)
('ITM0000000005', 'MER0000000002', '巨无霸', '经典双层牛肉汉堡', 2200, '汉堡', 'AVAILABLE', 1),
('ITM0000000006', 'MER0000000002', '麦辣鸡腿堡', '香辣鸡腿肉汉堡', 1800, '汉堡', 'AVAILABLE', 2),
('ITM0000000007', 'MER0000000002', '薯条大份', '金黄酥脆薯条', 1300, '小食', 'AVAILABLE', 3),
('ITM0000000008', 'MER0000000002', '麦乐鸡20块', '香脆鸡肉块', 2500, '小食', 'AVAILABLE', 4),
-- 海底捞火锅 (4个)
('ITM0000000009', 'MER0000000003', '精品肥牛', '优质牛肉，口感鲜嫩', 6800, '肉类', 'AVAILABLE', 1),
('ITM0000000010', 'MER0000000003', '虾滑', '新鲜虾肉制作', 4800, '海鲜', 'AVAILABLE', 2),
('ITM0000000011', 'MER0000000003', '毛肚', '新鲜毛肚，七上八下', 5800, '内脏', 'AVAILABLE', 3),
('ITM0000000012', 'MER0000000003', '土豆片', '薄切土豆片', 1200, '蔬菜', 'AVAILABLE', 4),
-- 外婆家 (4个)
('ITM0000000013', 'MER0000000004', '茶香鸡', '招牌茶香烤鸡', 4800, '招牌', 'AVAILABLE', 1),
('ITM0000000014', 'MER0000000004', '西湖醋鱼', '经典杭帮菜', 5800, '鱼类', 'AVAILABLE', 2),
('ITM0000000015', 'MER0000000004', '东坡肉', '肥而不腻，入口即化', 3800, '肉类', 'AVAILABLE', 3),
('ITM0000000016', 'MER0000000004', '龙井虾仁', '清新茶香虾仁', 4200, '海鲜', 'AVAILABLE', 4),
-- 必胜客 (4个)
('ITM0000000017', 'MER0000000005', '超级至尊披萨', '丰富配料，芝士拉丝', 10800, '披萨', 'AVAILABLE', 1),
('ITM0000000018', 'MER0000000005', '夏威夷风光披萨', '菠萝火腿，清爽美味', 8800, '披萨', 'AVAILABLE', 2),
('ITM0000000019', 'MER0000000005', '奶油蘑菇汤', '浓郁蘑菇香味', 2200, '汤品', 'AVAILABLE', 3),
('ITM0000000020', 'MER0000000005', '蜜汁烤翅', '香甜多汁', 2800, '小食', 'AVAILABLE', 4),
-- 肯德基 (4个)
('ITM0000000021', 'MER0000000006', '吮指原味鸡', '经典炸鸡', 1800, '炸鸡', 'AVAILABLE', 1),
('ITM0000000022', 'MER0000000006', '香辣鸡腿堡', '香辣可口', 1900, '汉堡', 'AVAILABLE', 2),
('ITM0000000023', 'MER0000000006', '葡式蛋挞', '香甜蛋挞4个', 1600, '甜品', 'AVAILABLE', 3),
('ITM0000000024', 'MER0000000006', '大份薯条', '金黄酥脆', 1200, '小食', 'AVAILABLE', 4),
-- 喜茶 (4个)
('ITM0000000025', 'MER0000000007', '多肉葡萄', '新鲜葡萄果肉', 2800, '果茶', 'AVAILABLE', 1),
('ITM0000000026', 'MER0000000007', '芝芝芒芒', '芒果芝士奶盖', 3000, '果茶', 'AVAILABLE', 2),
('ITM0000000027', 'MER0000000007', '烤黑糖波波', '黑糖珍珠奶茶', 2200, '奶茶', 'AVAILABLE', 3),
('ITM0000000028', 'MER0000000007', '多肉青提', '清爽青提果茶', 2600, '果茶', 'AVAILABLE', 4),
-- 瑞幸咖啡 (4个)
('ITM0000000029', 'MER0000000008', '生椰拿铁', '椰香浓郁', 2900, '咖啡', 'AVAILABLE', 1),
('ITM0000000030', 'MER0000000008', '厚乳拿铁', '浓郁奶香', 2700, '咖啡', 'AVAILABLE', 2),
('ITM0000000031', 'MER0000000008', '美式咖啡', '经典黑咖啡', 1800, '咖啡', 'AVAILABLE', 3),
('ITM0000000032', 'MER0000000008', '橙C美式', '清新橙香', 2200, '咖啡', 'AVAILABLE', 4),
-- 黄焖鸡米饭 (4个)
('ITM0000000033', 'MER0000000009', '黄焖鸡米饭', '招牌黄焖鸡配米饭', 2200, '招牌', 'AVAILABLE', 1),
('ITM0000000034', 'MER0000000009', '黄焖排骨饭', '黄焖排骨配米饭', 2500, '招牌', 'AVAILABLE', 2),
('ITM0000000035', 'MER0000000009', '冰镇可乐', '冰镇可口可乐', 500, '饮料', 'AVAILABLE', 3),
('ITM0000000036', 'MER0000000009', '紫菜蛋花汤', '清淡鲜美', 600, '汤品', 'AVAILABLE', 4),
-- 兰州拉面 (4个)
('ITM0000000037', 'MER0000000010', '牛肉拉面', '正宗兰州牛肉拉面', 1800, '面食', 'AVAILABLE', 1),
('ITM0000000038', 'MER0000000010', '牛肉炒拉面', '爆炒拉面', 2200, '面食', 'AVAILABLE', 2),
('ITM0000000039', 'MER0000000010', '牛肉盖浇饭', '牛肉浇头配米饭', 2500, '饭食', 'AVAILABLE', 3),
('ITM0000000040', 'MER0000000010', '凉拌牛肉', '凉拌牛肉片', 2800, '凉菜', 'AVAILABLE', 4),
-- 呷哺呷哺 (4个)
('ITM0000000041', 'MER0000000011', '经典牛肉套餐', '牛肉加蔬菜拼盘', 5800, '套餐', 'AVAILABLE', 1),
('ITM0000000042', 'MER0000000011', '羊肉套餐', '羊肉加蔬菜拼盘', 6200, '套餐', 'AVAILABLE', 2),
('ITM0000000043', 'MER0000000011', '番茄锅底', '番茄锅底', 1800, '锅底', 'AVAILABLE', 3),
('ITM0000000044', 'MER0000000011', '麻辣锅底', '麻辣锅底', 2200, '锅底', 'AVAILABLE', 4),
-- 真功夫 (4个)
('ITM0000000045', 'MER0000000012', '冬菇鸡腿肉饭', '蒸制鸡腿肉', 2800, '套餐', 'AVAILABLE', 1),
('ITM0000000046', 'MER0000000012', '排骨蒸饭', '蒸制排骨', 3200, '套餐', 'AVAILABLE', 2),
('ITM0000000047', 'MER0000000012', '香滑蒸蛋', '嫩滑蒸蛋', 800, '小食', 'AVAILABLE', 3),
('ITM0000000048', 'MER0000000012', '营养例汤', '营养例汤', 600, '汤品', 'AVAILABLE', 4),
-- 赛百味 (4个)
('ITM0000000049', 'MER0000000013', '意大利香辣', '经典意式三明治', 3200, '三明治', 'AVAILABLE', 1),
('ITM0000000050', 'MER0000000013', '金枪鱼三明治', '金枪鱼沙拉三明治', 3500, '三明治', 'AVAILABLE', 2),
('ITM0000000051', 'MER0000000013', '巧克力曲奇', '巧克力曲奇', 800, '甜品', 'AVAILABLE', 3),
('ITM0000000052', 'MER0000000013', '香脆薯条', '香脆薯条', 1000, '小食', 'AVAILABLE', 4),
-- 吉野家 (4个)
('ITM0000000053', 'MER0000000014', '招牌牛肉饭', '经典日式牛肉饭', 2800, '饭食', 'AVAILABLE', 1),
('ITM0000000054', 'MER0000000014', '照烧鸡腿饭', '照烧风味鸡腿', 2500, '饭食', 'AVAILABLE', 2),
('ITM0000000055', 'MER0000000014', '味噌汤', '日式味噌汤', 500, '汤品', 'AVAILABLE', 3),
('ITM0000000056', 'MER0000000014', '日式炸鸡', '酥脆炸鸡块', 1500, '小食', 'AVAILABLE', 4),
-- 张亮麻辣烫 (4个)
('ITM0000000057', 'MER0000000015', '麻辣烫中份', '自选菜品麻辣烫', 2500, '麻辣烫', 'AVAILABLE', 1),
('ITM0000000058', 'MER0000000015', '麻辣烫大份', '自选菜品麻辣烫', 3500, '麻辣烫', 'AVAILABLE', 2),
('ITM0000000059', 'MER0000000015', '酸辣粉', '经典酸辣粉', 1500, '粉面', 'AVAILABLE', 3),
('ITM0000000060', 'MER0000000015', '冰粉', '解辣冰粉', 600, '甜品', 'AVAILABLE', 4),
-- CoCo都可 (4个)
('ITM0000000061', 'MER0000000016', '奶茶三兄弟', '奶茶加布丁珍珠仙草', 1600, '奶茶', 'AVAILABLE', 1),
('ITM0000000062', 'MER0000000016', '鲜芋奶茶', '芋头奶茶', 1800, '奶茶', 'AVAILABLE', 2),
('ITM0000000063', 'MER0000000016', '百香果双响炮', '百香果椰果', 1500, '果茶', 'AVAILABLE', 3),
('ITM0000000064', 'MER0000000016', '柠檬霸', '大杯柠檬水', 1200, '果茶', 'AVAILABLE', 4);
