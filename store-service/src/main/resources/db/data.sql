INSERT INTO app_user (username, password, enabled, roles) VALUES
('admin',
'{bcrypt}$2a$10$1PoMl1vMSoPDnhtJSAjW3utizMecIGGyrOwjL0AEK1ckNLDGKt5vG', -- password
TRUE,
ARRAY['ROLE_ADMIN']
),
('test',
 '{bcrypt}$2a$10$wOYmQcE/gY3yYlhc2xdmc.rd4G0Pwlg24EKI65fFwmWf.1sWtgQB6', -- 123456789
 TRUE,
 ARRAY ['ROLE_USER'])
ON CONFLICT (username) DO NOTHING;

INSERT INTO items (title, description, price, img_path) VALUES
('Item 1', 'Description for item 1', 10.99, '/images/item1.jpg'),
('Item 2', 'Description for item 2', 15.49, '/images/item2.jpg'),
('Item 3', 'Description for item 3', 7.99, '/images/item3.jpg'),
('Item 4', 'Description for item 4', 12.00, '/images/item4.jpg'),
('Item 5', 'Description for item 5', 20.00, '/images/item5.jpg'),
('Item 6', 'Description for item 6', 5.50, '/images/item6.jpg'),
('Item 7', 'Description for item 7', 8.75, '/images/item7.jpg'),
('Item 8', 'Description for item 8', 14.99, '/images/item8.jpg'),
('Item 9', 'Description for item 9', 11.25, '/images/item9.jpg'),
('Item 10', 'Description for item 10', 9.99, '/images/item10.jpg');
