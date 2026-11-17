const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_1";
const BOB_UID = "bob_teacher_2";
const EVE_UID = "eve_outsider_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function makeSchoolPayload(ownerUid, joinCode = "KGP482", approvalRequired = false) {
  const now = pastDate(5000);
  return {
    ownerUid,
    joinCode,
    approvalRequired,
    memberUids: [ownerUid],
    adminUids: [ownerUid],
    memberProfiles: {
      [ownerUid]: {
        uid: ownerUid,
        name: "মোঃ আকিবুল ইসলাম",
        email: "akibul@school.edu.bd",
        role: "ADMIN",
      },
    },
    school: "কামারখালী সরকারি প্রাথমিক বিদ্যালয়",
    schoolAddress: "মধুখালী, ফরিদপুর",
    year: "২০২৬",
    teachers: { "1": "তাসলিমা খাতুন" },
    teacherMobiles: { "1": "01711111111" },
    pdfFont: "noto_serif",
    dpeLogo: "simple",
    customFields: [{ key: "cf_blood", label: "রক্তের গ্রুপ" }],
    createdAt: now,
    updatedAt: now,
  };
}

function makeCodePayload(schoolId, ownerUid, schoolName = "কামারখালী সরকারি প্রাথমিক বিদ্যালয়", approvalRequired = false) {
  const now = pastDate(5000);
  return {
    schoolId,
    schoolName,
    ownerUid,
    adminUids: [ownerUid],
    approvalRequired,
    active: true,
    createdAt: now,
    updatedAt: now,
  };
}

function makeStudentPayload(updatedByUid, roll = 1, name = "তানভীর আহমেদ") {
  const now = pastDate(3000);
  return {
    cls: 1,
    roll,
    name,
    guardian: "মোঃ করিম উদ্দিন",
    village: "কামারখালী",
    post: "কামারখালী",
    upazila: "মধুখালী",
    district: "ফরিদপুর",
    mobile: "01711223344",
    religion: "ইসলাম",
    m: { BAN_1_TB: 18, BAN_1_WR: 44 },
    x: { DAYS_1: "62", PRES_1: "59", ACT_1: "ক" },
    updatedByUid,
    updatedByName: "শিক্ষক",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("schoolCodes").doc("KGP482").get());
  await assertFails(unauthDb.collection("schools").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("schools").doc("school_1").collection("students").get());
});

test("User profile PII is isolated strictly to the owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      activeSchoolId: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read or update Alice's PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate(500) }));

  // Alice cannot submit extra ghost field or future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice 2",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Admin can atomically create school + joinCode, and invite codes cannot be listed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("schools").doc("school_1"), makeSchoolPayload(ALICE_UID, "KGP482", false));
  batch.set(aliceDb.collection("schoolCodes").doc("KGP482"), makeCodePayload("school_1", ALICE_UID));
  await assertSucceeds(batch.commit());

  // Anyone signed in can look up a known code by exact ID, but CANNOT list all codes
  await assertSucceeds(bobDb.collection("schoolCodes").doc("KGP482").get());
  await assertFails(bobDb.collection("schoolCodes").get());

  // Non-member cannot read school workspace or list unfiltered schools
  await assertFails(bobDb.collection("schools").doc("school_1").get());
  await assertFails(bobDb.collection("schools").get());
});

test("Direct self-join works when approvalRequired is false, and blocks role escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const createBatch = aliceDb.batch();
  createBatch.set(aliceDb.collection("schools").doc("school_1"), makeSchoolPayload(ALICE_UID, "KGP482", false));
  createBatch.set(aliceDb.collection("schoolCodes").doc("KGP482"), makeCodePayload("school_1", ALICE_UID));
  await assertSucceeds(createBatch.commit());

  // Bob cannot self-assign ADMIN role when joining
  await assertFails(
    bobDb.collection("schools").doc("school_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID, BOB_UID],
      memberProfiles: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", email: "a@x.com", role: "ADMIN" },
        [BOB_UID]: { uid: BOB_UID, name: "Bob", email: "b@x.com", role: "ADMIN" },
      },
      updatedAt: pastDate(500),
    })
  );

  // Valid self-join as TEACHER succeeds
  await assertSucceeds(
    bobDb.collection("schools").doc("school_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      "memberProfiles.bob_teacher_2": {
        uid: BOB_UID,
        name: "বব শিক্ষক",
        email: "bob@school.edu.bd",
        role: "TEACHER",
      },
      updatedAt: pastDate(500),
    })
  );

  // Bob can now query his schools via memberUids filter
  await assertSucceeds(
    bobDb.collection("schools").where("memberUids", "array-contains", BOB_UID).get()
  );
});

test("Concurrent student field-level updates succeed for members, and outsiders are blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  // Seed school with Alice + Bob as members
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "KGP482", false);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = { uid: BOB_UID, name: "Bob", email: "bob@x.com", role: "TEACHER" };
    await adminDb.collection("schools").doc("school_1").set(school);
    await adminDb.collection("schoolCodes").doc("KGP482").set(makeCodePayload("school_1", ALICE_UID));
  });

  // Alice creates a student (NO @DocumentId id field)
  const studentRefAlice = aliceDb.collection("schools").doc("school_1").collection("students").doc("st_1");
  await assertSucceeds(studentRefAlice.set(makeStudentPayload(ALICE_UID, 1, "নুসরাত")));

  // Reject creating student with extra `id` field
  await assertFails(
    aliceDb.collection("schools").doc("school_1").collection("students").doc("st_bad").set({
      id: "st_bad",
      ...makeStudentPayload(ALICE_UID, 2, "Test"),
    })
  );

  // Bob (member teacher) updates MATH_1_TB field via dot-notation without overwriting BAN_1_TB
  const studentRefBob = bobDb.collection("schools").doc("school_1").collection("students").doc("st_1");
  await assertSucceeds(
    studentRefBob.update({
      "m.MATH_1_TB": 19,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(400),
    })
  );

  // Bob cannot mutate createdAt
  await assertFails(
    studentRefBob.update({
      createdAt: pastDate(100),
      updatedByUid: BOB_UID,
      updatedAt: pastDate(100),
    })
  );

  // Eve (non-member) cannot list, read, create, update, or delete students
  await assertFails(eveDb.collection("schools").doc("school_1").collection("students").get());
  await assertFails(eveDb.collection("schools").doc("school_1").collection("students").doc("st_1").get());
  await assertFails(
    eveDb.collection("schools").doc("school_1").collection("students").doc("st_1").update({
      "m.MATH_1_TB": 0,
      updatedByUid: EVE_UID,
      updatedAt: pastDate(200),
    })
  );
  await assertFails(eveDb.collection("schools").doc("school_1").collection("students").doc("st_1").delete());

  // Teacher can leave school; afterwards Bob immediately loses access to students
  await assertSucceeds(
    bobDb.collection("schools").doc("school_1").update({
      memberUids: [ALICE_UID],
      memberProfiles: {
        [ALICE_UID]: { uid: ALICE_UID, name: "মোঃ আকিবুল ইসলাম", email: "akibul@school.edu.bd", role: "ADMIN" },
      },
      updatedAt: pastDate(100),
    })
  );
  await assertFails(bobDb.collection("schools").doc("school_1").collection("students").get());
});

test("Approval-required mode enforces joinRequests flow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const createBatch = aliceDb.batch();
  createBatch.set(aliceDb.collection("schools").doc("school_2"), makeSchoolPayload(ALICE_UID, "APR999", true));
  createBatch.set(aliceDb.collection("schoolCodes").doc("APR999"), makeCodePayload("school_2", ALICE_UID, "টেস্ট স্কুল", true));
  await assertSucceeds(createBatch.commit());

  // Bob cannot self-join when approvalRequired == true
  await assertFails(
    bobDb.collection("schools").doc("school_2").update({
      memberUids: [ALICE_UID, BOB_UID],
      "memberProfiles.bob_teacher_2": { uid: BOB_UID, name: "Bob", email: "bob@x.com", role: "TEACHER" },
      updatedAt: pastDate(500),
    })
  );

  // Bob submits a joinRequest
  const now = pastDate(1000);
  await assertSucceeds(
    bobDb.collection("schools").doc("school_2").collection("joinRequests").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      email: "bob@x.com",
      schoolId: "school_2",
      schoolName: "টেস্ট স্কুল",
      schoolAdminUids: [ALICE_UID],
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Outsider Eve cannot list Alice's school joinRequests
  await assertFails(
    eveDb.collection("schools").doc("school_2").collection("joinRequests").where("schoolAdminUids", "array-contains", ALICE_UID).get()
  );

  // Admin Alice can list joinRequests for her school
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_2").collection("joinRequests").where("schoolAdminUids", "array-contains", ALICE_UID).get()
  );

  // Admin approves Bob atomically & deletes request
  const approveBatch = aliceDb.batch();
  approveBatch.update(aliceDb.collection("schools").doc("school_2"), {
    memberUids: [ALICE_UID, BOB_UID],
    "memberProfiles.bob_teacher_2": { uid: BOB_UID, name: "Bob", email: "bob@x.com", role: "TEACHER" },
    updatedAt: pastDate(300),
  });
  approveBatch.delete(aliceDb.collection("schools").doc("school_2").collection("joinRequests").doc(BOB_UID));
  await assertSucceeds(approveBatch.commit());
});
