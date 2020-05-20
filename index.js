//
const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.database();

exports.sendNotification = functions.database.ref('/Notification/{userId}/{notificationId}')
.onCreate(async (snapshot, context) => {
    let receiver = context.params.userId;
    let getToken = await db.ref('User/'+receiver+'/notificationToken').once('value');
    let token = getToken.val();
    if(!token)
        return console.log('No register device');
    let listToken = [];
    for(let key in token) {
        listToken.push(key);
    }
    let noti = snapshot.val();
    let sender = noti.userId;
    let getSender = await db.ref('User/'+sender).once('value');
    let username = getSender.val().username;
    let profileImage = getSender.val().avatarUrl;
    let payload = {
        data: {
            userId: sender,
            username: username,
            content: noti.content,
            contentId: noti.contentId,
            type: noti.type.toString(),
            urlImage: profileImage
        }
    };
    return admin.messaging().sendToDevice(listToken, payload);
});

exports.addPost = functions.database.ref('Post/{postId}')
.onCreate(async (snapshot, context) => {
    let ref = db.ref('PostMain');
    let postId = context.params.postId;
    let post = snapshot.val();
    let publisherId = post.publisherId;
    let getFollower = await db.ref('Follow/'+publisherId+'/Follower').once('value');
    let listFollower = getFollower.val();
    for(let key in listFollower) {
        ref.child(key).child(postId).set(true);
    }
});

exports.deletePost = functions.database.ref('Post/{postId}')
.onDelete(async (snapshot, context) => {
    let ref = db.ref('PostMain');
    let postId = context.params.postId;
    let post = snapshot.val();
    let publisherId = post.publisherId;

    let getFollower = await db.ref('Follow/'+publisherId+'/Follower').once('value');
    let listFollower = getFollower.val();
    for(let key in listFollower) {
        ref.child(key).child(postId).remove();
        db.ref('Save/'+key).child(postId).remove();
    }

    let getNotification = await db.ref('Notification/'+publisherId).orderByChild('contentId').equalTo(postId).once('value');
    let listNoti = getNotification.val();
    for(let key in listNoti) {
        db.ref('Notification/'+publisherId).child(key).remove();
    }
});

exports.addNewFollowerToPostAndStory = functions.database.ref('Follow/{userId}/Follower/{followerId}')
.onCreate(async (snapshot, context) => {
    let userId = context.params.userId;
    let followerId = context.params.followerId;

    let getPost = await db.ref('User/'+userId+'/Post').once('value');
    let listPost = getPost.val();
    let ref1 = db.ref('PostMain/'+followerId);
    for(let key in listPost) {
        ref1.child(key).set(true);
    }

    let getStory = await db.ref('User/'+userId+'/Story').once('value');
    let listStory = getStory.val();
    let ref2 = db.ref('StoryMain/'+followerId + '/' +userId);
    for(let key in listStory) {
        ref2.child(key).set(false);
        ref2.child('lastest').set(key);
    }

});

exports.removeFollowerFromPostAndStory = functions.database.ref('Follow/{userId}/Follower/{followerId}')
.onDelete(async (snapshot, context) => {
    let userId = context.params.userId;
    let followerId = context.params.followerId;
    let getPost = await db.ref('User/'+userId+'/Post').once('value');
    let listPost = getPost.val();
    let ref1 = db.ref('PostMain/'+followerId);
    for(let key in listPost) {
        ref1.child(key).remove();
    }

    let ref2 = db.ref('StoryMain/'+followerId);
    ref2.child(userId).remove();

});

exports.addStory = functions.database.ref('Story/{storyId}')
.onCreate(async (snapshot, context) => {
    let ref = db.ref('StoryMain');
    let storyId = context.params.storyId;
    let story = snapshot.val();
    let userId = story.userId;
    let getFollower = await db.ref('Follow/'+userId+'/Follower').once('value');
    let listFollower = getFollower.val();
    for(let key in listFollower) {
        ref.child(key).child(userId).child(storyId).set(false);
        ref.child(key).child(userId).child('lastest').set(storyId);
    }
});



