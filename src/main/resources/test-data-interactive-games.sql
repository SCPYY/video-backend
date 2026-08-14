-- Three complete interactive games with public MP4 URLs.
SET @category_id = (SELECT id FROM content_categories WHERE type = 2 AND status = 1 ORDER BY id LIMIT 1);

INSERT INTO contents (`type`,title,description,cover_url,category,category_id,tags,status,content_status,view_count,play_count,unique_view_count,play_user_count,like_count,favorite_count,share_count,comment_count,danmaku_count,sort_order,created_by,updated_by)
SELECT 2,'INTERACTIVE_TEST_MYSTERY','Interactive mystery branching story','https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg','INTERACTIVE',@category_id,'mystery,choice,ending',1,4,0,0,0,0,0,0,0,0,0,100,2,2
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='INTERACTIVE_TEST_MYSTERY' AND status>=0);
SET @c1=(SELECT id FROM contents WHERE title='INTERACTIVE_TEST_MYSTERY' AND status>=0 ORDER BY id DESC LIMIT 1);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c1,1,'Mysterious Message','Opening scene','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',15,'NORMAL',1,1,0,0 WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes WHERE content_id=@c1 AND scene_no=1);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c1,2,'Follow the Clue','Branch scene','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',15,'NORMAL',0,1,0,0 WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes WHERE content_id=@c1 AND scene_no=2);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c1,3,'Truth Ending','Good ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',60,'ENDING',0,1,0,0 WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes WHERE content_id=@c1 AND scene_no=3);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c1,4,'Escape Ending','Alternative ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',15,'ENDING',0,1,0,0 WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes WHERE content_id=@c1 AND scene_no=4);
SET @c1s1=(SELECT id FROM interactive_scenes WHERE content_id=@c1 AND scene_no=1); SET @c1s2=(SELECT id FROM interactive_scenes WHERE content_id=@c1 AND scene_no=2); SET @c1s3=(SELECT id FROM interactive_scenes WHERE content_id=@c1 AND scene_no=3); SET @c1s4=(SELECT id FROM interactive_scenes WHERE content_id=@c1 AND scene_no=4);
INSERT INTO interactive_nodes(scene_id,node_no,prompt,node_type,show_at,timeout_seconds,required)
SELECT @c1s1,1,'How will you respond?','SINGLE',8,15,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_nodes WHERE scene_id=@c1s1 AND node_no=1);
SET @c1n=(SELECT id FROM interactive_nodes WHERE scene_id=@c1s1 AND node_no=1);
INSERT INTO interactive_options(node_id,option_no,title,description,next_scene_id,status) SELECT @c1n,1,'Follow the clue','Continue investigating',@c1s2,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c1n AND option_no=1);
INSERT INTO interactive_options(node_id,option_no,title,description,next_scene_id,status) SELECT @c1n,2,'Leave immediately','Choose a safer ending',@c1s4,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c1n AND option_no=2);
INSERT INTO interactive_nodes(scene_id,node_no,prompt,node_type,show_at,timeout_seconds,required)
SELECT @c1s2,1,'Reveal the truth?','SINGLE',8,15,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_nodes WHERE scene_id=@c1s2 AND node_no=1);
SET @c1n2=(SELECT id FROM interactive_nodes WHERE scene_id=@c1s2 AND node_no=1);
INSERT INTO interactive_options(node_id,option_no,title,description,next_scene_id,status) SELECT @c1n2,1,'Reveal it','Reach the truth ending',@c1s3,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c1n2 AND option_no=1);
INSERT INTO interactive_options(node_id,option_no,title,description,next_scene_id,status) SELECT @c1n2,2,'Walk away','Reach the escape ending',@c1s4,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c1n2 AND option_no=2);

INSERT INTO contents (`type`,title,description,cover_url,category,category_id,tags,status,content_status,view_count,play_count,unique_view_count,play_user_count,like_count,favorite_count,share_count,comment_count,danmaku_count,sort_order,created_by,updated_by)
SELECT 2,'INTERACTIVE_TEST_ROMANCE','Interactive romance with two endings','https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg','INTERACTIVE',@category_id,'romance,choice,ending',1,4,0,0,0,0,0,0,0,0,0,90,2,2
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='INTERACTIVE_TEST_ROMANCE' AND status>=0);
SET @c2=(SELECT id FROM contents WHERE title='INTERACTIVE_TEST_ROMANCE' AND status>=0 ORDER BY id DESC LIMIT 1);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c2,n,title,description,url,duration,scene_type,is_start,1,0,0 FROM (
 SELECT 1 n,'First Meeting' title,'Opening' description,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4' url,15 duration,'NORMAL' scene_type,1 is_start
 UNION ALL SELECT 2,'Confession','Branch','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',15,'NORMAL',0
 UNION ALL SELECT 3,'Together Ending','Happy ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',60,'ENDING',0
 UNION ALL SELECT 4,'Friends Ending','Alternative ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',15,'ENDING',0) x
WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes s WHERE s.content_id=@c2 AND s.scene_no=x.n);
SET @c2s1=(SELECT id FROM interactive_scenes WHERE content_id=@c2 AND scene_no=1);SET @c2s3=(SELECT id FROM interactive_scenes WHERE content_id=@c2 AND scene_no=3);SET @c2s4=(SELECT id FROM interactive_scenes WHERE content_id=@c2 AND scene_no=4);
INSERT INTO interactive_nodes(scene_id,node_no,prompt,node_type,show_at,timeout_seconds,required) SELECT @c2s1,1,'What do you say?','SINGLE',8,15,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_nodes WHERE scene_id=@c2s1 AND node_no=1);
SET @c2n=(SELECT id FROM interactive_nodes WHERE scene_id=@c2s1 AND node_no=1);
INSERT INTO interactive_options(node_id,option_no,title,next_scene_id,status) SELECT @c2n,1,'Confess your feelings',@c2s3,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c2n AND option_no=1);
INSERT INTO interactive_options(node_id,option_no,title,next_scene_id,status) SELECT @c2n,2,'Stay friends',@c2s4,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c2n AND option_no=2);

INSERT INTO contents (`type`,title,description,cover_url,category,category_id,tags,status,content_status,view_count,play_count,unique_view_count,play_user_count,like_count,favorite_count,share_count,comment_count,danmaku_count,sort_order,created_by,updated_by)
SELECT 2,'INTERACTIVE_TEST_ADVENTURE','Interactive adventure with branching endings','https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg','INTERACTIVE',@category_id,'adventure,choice,ending',1,4,0,0,0,0,0,0,0,0,0,80,2,2
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='INTERACTIVE_TEST_ADVENTURE' AND status>=0);
SET @c3=(SELECT id FROM contents WHERE title='INTERACTIVE_TEST_ADVENTURE' AND status>=0 ORDER BY id DESC LIMIT 1);
INSERT INTO interactive_scenes(content_id,scene_no,title,description,video_url,duration,scene_type,is_start,status,view_count,play_count)
SELECT @c3,n,title,description,url,duration,scene_type,is_start,1,0,0 FROM (
 SELECT 1 n,'Ancient Gate' title,'Opening' description,'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4' url,15 duration,'NORMAL' scene_type,1 is_start
 UNION ALL SELECT 2,'Hidden Path','Branch','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',15,'NORMAL',0
 UNION ALL SELECT 3,'Treasure Ending','Victory ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',60,'ENDING',0
 UNION ALL SELECT 4,'Return Ending','Safe ending','https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',15,'ENDING',0) x
WHERE NOT EXISTS(SELECT 1 FROM interactive_scenes s WHERE s.content_id=@c3 AND s.scene_no=x.n);
SET @c3s1=(SELECT id FROM interactive_scenes WHERE content_id=@c3 AND scene_no=1);SET @c3s3=(SELECT id FROM interactive_scenes WHERE content_id=@c3 AND scene_no=3);SET @c3s4=(SELECT id FROM interactive_scenes WHERE content_id=@c3 AND scene_no=4);
INSERT INTO interactive_nodes(scene_id,node_no,prompt,node_type,show_at,timeout_seconds,required) SELECT @c3s1,1,'Choose your path','SINGLE',8,15,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_nodes WHERE scene_id=@c3s1 AND node_no=1);
SET @c3n=(SELECT id FROM interactive_nodes WHERE scene_id=@c3s1 AND node_no=1);
INSERT INTO interactive_options(node_id,option_no,title,next_scene_id,status) SELECT @c3n,1,'Enter the gate',@c3s3,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c3n AND option_no=1);
INSERT INTO interactive_options(node_id,option_no,title,next_scene_id,status) SELECT @c3n,2,'Return home',@c3s4,1 WHERE NOT EXISTS(SELECT 1 FROM interactive_options WHERE node_id=@c3n AND option_no=2);
