const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_vybe_1";
const BOB_UID = "bob_vybe_2";
const CHARLIE_UID = "charlie_vybe_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedProfilesAndAccount() {
  const ts = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("accounts").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      alertsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rivera",
      handle: "alice_r",
      bio: "Soundscapes & late-night design",
      moodTag: "Night Owl",
      avatarColor: "coral",
      discoverable: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Chen",
      handle: "bob_c",
      bio: "Analog synths & espresso",
      moodTag: "Studio",
      avatarColor: "violet",
      discoverable: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated users: denied across all collections", async () => {
  await seedProfilesAndAccount();
  const unauth = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauth.collection("accounts").doc(ALICE_UID).get());
  await assertFails(unauth.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(unauth.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("stories").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("likes").where("userId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("follows").where("followerId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("notifications").get());
});

test("Accounts PII isolation: owner read/write succeeds, cross-user read/write fails", async () => {
  await seedProfilesAndAccount();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(2000);

  await assertSucceeds(alice.collection("accounts").doc(ALICE_UID).get());
  await assertSucceeds(
    alice.collection("accounts").doc(ALICE_UID).update({
      alertsEnabled: false,
      updatedAt: ts,
    })
  );

  await assertFails(bob.collection("accounts").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("accounts").doc(ALICE_UID).update({ alertsEnabled: false, updatedAt: ts })
  );
  await assertFails(
    alice.collection("accounts").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: ts,
    })
  );
  await assertFails(
    alice.collection("accounts").doc(ALICE_UID).update({
      alertsEnabled: true,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Profiles & Follows: filtered queries, owner edits, and atomic follow/unfollow", async () => {
  await seedProfilesAndAccount();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(2000);

  await assertSucceeds(alice.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(alice.collection("profiles").get());

  await assertSucceeds(
    alice.collection("profiles").doc(ALICE_UID).update({
      bio: "Updated creative bio",
      moodTag: "Creative",
      updatedAt: ts,
    })
  );
  await assertFails(
    bob.collection("profiles").doc(ALICE_UID).update({
      bio: "Hijacked",
      updatedAt: ts,
    })
  );

  // Alice follows Bob atomically
  const followId = `${ALICE_UID}_${BOB_UID}`;
  const followBatch = alice.batch();
  followBatch.set(alice.collection("follows").doc(followId), {
    followerId: ALICE_UID,
    followedId: BOB_UID,
    followerName: "Alice Rivera",
    followedName: "Bob Chen",
    createdAt: ts,
  });
  followBatch.update(alice.collection("profiles").doc(ALICE_UID), {
    followingCount: 1,
    updatedAt: ts,
  });
  followBatch.update(alice.collection("profiles").doc(BOB_UID), {
    followersCount: 1,
    updatedAt: ts,
  });
  await assertSucceeds(followBatch.commit());

  await assertSucceeds(alice.collection("follows").where("followerId", "==", ALICE_UID).get());
  await assertSucceeds(bob.collection("follows").where("followedId", "==", BOB_UID).get());

  // Unauthenticated or spoofed follow counter update without follow edge fails
  await assertFails(
    alice.collection("profiles").doc(BOB_UID).update({
      followersCount: 99,
      updatedAt: pastDate(1000),
    })
  );

  // Alice unfollows Bob
  const unfollowBatch = alice.batch();
  unfollowBatch.delete(alice.collection("follows").doc(followId));
  unfollowBatch.update(alice.collection("profiles").doc(ALICE_UID), {
    followingCount: 0,
    updatedAt: pastDate(500),
  });
  unfollowBatch.update(alice.collection("profiles").doc(BOB_UID), {
    followersCount: 0,
    updatedAt: pastDate(500),
  });
  await assertSucceeds(unfollowBatch.commit());
});

test("Posts, Likes, Comments & Stories: create, filtered queries, atomic interactions", async () => {
  await seedProfilesAndAccount();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(4000);

  // Create post
  const batchCreate = alice.batch();
  const postRef = alice.collection("posts").doc("post_1");
  batchCreate.set(postRef, {
    authorId: ALICE_UID,
    authorName: "Alice Rivera",
    authorHandle: "alice_r",
    authorAvatarColor: "coral",
    caption: "Midnight synth jam in the studio",
    vibeTag: "Music",
    mediaType: "text",
    mediaDurationSec: 0,
    visibility: "public",
    likesCount: 0,
    commentsCount: 0,
    createdAt: ts,
    updatedAt: ts,
  });
  batchCreate.update(alice.collection("profiles").doc(ALICE_UID), {
    postsCount: 1,
    updatedAt: ts,
  });
  await assertSucceeds(batchCreate.commit());

  // Create story
  await assertSucceeds(
    alice.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorName: "Alice Rivera",
      authorHandle: "alice_r",
      authorAvatarColor: "coral",
      caption: "Live from studio B",
      moodTag: "Night Owl",
      mediaType: "text",
      accentGradient: "coral",
      visibility: "public",
      expiresAtEpochMs: Date.now() + 86400000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bob.collection("stories").where("visibility", "==", "public").get());

  // Bob likes & comments atomically
  const bobPostRef = bob.collection("posts").doc("post_1");
  const likeId = `post_1_${BOB_UID}`;
  const likeBatch = bob.batch();
  likeBatch.set(bob.collection("likes").doc(likeId), {
    postId: "post_1",
    userId: BOB_UID,
    postAuthorId: ALICE_UID,
    createdAt: pastDate(2000),
  });
  likeBatch.update(bobPostRef, {
    likesCount: 1,
    updatedAt: pastDate(2000),
  });
  await assertSucceeds(likeBatch.commit());

  const commentBatch = bob.batch();
  commentBatch.set(bobPostRef.collection("comments").doc("comment_1"), {
    postId: "post_1",
    postVisibility: "public",
    authorId: BOB_UID,
    authorName: "Bob Chen",
    authorHandle: "bob_c",
    authorAvatarColor: "violet",
    text: "This tone is unreal!",
    createdAt: pastDate(1500),
  });
  commentBatch.update(bobPostRef, {
    commentsCount: 1,
    updatedAt: pastDate(1500),
  });
  await assertSucceeds(commentBatch.commit());

  await assertSucceeds(
    bob.collection("posts").doc("post_1").collection("comments").where("postVisibility", "==", "public").get()
  );

  // Bob cannot delete Alice's post or story
  await assertFails(bob.collection("posts").doc("post_1").delete());
  await assertFails(bob.collection("stories").doc("story_1").delete());

  // Alice can delete her story and post
  await assertSucceeds(alice.collection("stories").doc("story_1").delete());
  await assertSucceeds(alice.collection("posts").doc("post_1").delete());
});

test("Direct Messages & Notifications: participant isolation and cross-user notification create", async () => {
  await seedProfilesAndAccount();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const convId = `${ALICE_UID}_${BOB_UID}`;
  const ts = pastDate(3000);

  const msgBatch = alice.batch();
  const convRef = alice.collection("conversations").doc(convId);
  msgBatch.set(convRef, {
    participantIds: [ALICE_UID, BOB_UID],
    participantAId: ALICE_UID,
    participantAName: "Alice Rivera",
    participantAHandle: "alice_r",
    participantBId: BOB_UID,
    participantBName: "Bob Chen",
    participantBHandle: "bob_c",
    lastMessageText: "Hey Bob, love the synth track!",
    lastSenderId: ALICE_UID,
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
  });
  msgBatch.set(convRef.collection("messages").doc("msg_1"), {
    conversationId: convId,
    senderId: ALICE_UID,
    senderName: "Alice Rivera",
    participantIds: [ALICE_UID, BOB_UID],
    text: "Hey Bob, love the synth track!",
    createdAt: ts,
  });
  msgBatch.set(
    alice.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1"),
    {
      recipientId: BOB_UID,
      actorId: ALICE_UID,
      actorName: "Alice Rivera",
      type: "message",
      title: "New message from Alice Rivera",
      body: "Hey Bob, love the synth track!",
      targetId: convId,
      seen: false,
      createdAt: ts,
    }
  );
  await assertSucceeds(msgBatch.commit());

  await assertSucceeds(
    bob.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bob
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Charlie cannot read or write conversation or messages
  await assertFails(charlie.collection("conversations").doc(convId).get());
  await assertFails(
    charlie.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlie
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );

  // Bob reads & marks notification seen
  await assertSucceeds(bob.collection("users").doc(BOB_UID).collection("notifications").get());
  await assertSucceeds(
    bob.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").update({ seen: true })
  );

  // Charlie cannot read Bob's notifications or spoof actorId
  await assertFails(charlie.collection("users").doc(BOB_UID).collection("notifications").get());
  await assertFails(
    charlie
      .collection("users")
      .doc(BOB_UID)
      .collection("notifications")
      .doc("notif_spoof")
      .set({
        recipientId: BOB_UID,
        actorId: ALICE_UID,
        actorName: "Alice Rivera",
        type: "like",
        title: "Spoof",
        body: "Spoofed notification",
        seen: false,
        createdAt: pastDate(500),
      })
  );
});
