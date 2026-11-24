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

function makeSchoolPayload(ownerUid, joinCode = "KGP482", approvalRequired = true) {
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
        allClasses: true,
        allowedClasses: [1, 2, 3, 4, 5],
        allSubjects: true,
        allowedSubjects: ["BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART"],
        canViewOtherSubjects: true,
        canEditAttendance: true,
        canManageStudents: true,
        canExportPdf: true,
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

function makeCodePayload(schoolId, ownerUid, schoolName = "কামারখালী সরকারি প্রাথমিক বিদ্যালয়", approvalRequired = true) {
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

function makeStudentPayload(updatedByUid, roll = 1, name = "তানভীর আহমেদ", cls = 1) {
  const now = pastDate(3000);
  return {
    cls,
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
      pendingSchoolId: "",
      pendingSchoolName: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate(500) }));

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

test("Single school per user: user with activeSchoolId cannot create or request another school", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(4000);

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

  // Create first school atomically
  const batch1 = aliceDb.batch();
  batch1.set(aliceDb.collection("schools").doc("school_1"), makeSchoolPayload(ALICE_UID, "KGP482", true));
  batch1.set(aliceDb.collection("schoolCodes").doc("KGP482"), makeCodePayload("school_1", ALICE_UID));
  batch1.update(aliceDb.collection("users").doc(ALICE_UID), { activeSchoolId: "school_1", updatedAt: pastDate(1000) });
  await assertSucceeds(batch1.commit());

  // Creating a second school must fail
  const batch2 = aliceDb.batch();
  batch2.set(aliceDb.collection("schools").doc("school_2"), makeSchoolPayload(ALICE_UID, "KGP999", true));
  batch2.set(aliceDb.collection("schoolCodes").doc("KGP999"), makeCodePayload("school_2", ALICE_UID));
  await assertFails(batch2.commit());
});

test("Admin approval is mandatory for joining existing school", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const createBatch = aliceDb.batch();
  createBatch.set(aliceDb.collection("schools").doc("school_2"), makeSchoolPayload(ALICE_UID, "APR999", true));
  createBatch.set(aliceDb.collection("schoolCodes").doc("APR999"), makeCodePayload("school_2", ALICE_UID, "টেস্ট স্কুল", true));
  await assertSucceeds(createBatch.commit());

  // Bob cannot self-join without admin approval
  await assertFails(
    bobDb.collection("schools").doc("school_2").update({
      memberUids: [ALICE_UID, BOB_UID],
      "memberProfiles.bob_teacher_2": { uid: BOB_UID, name: "Bob", email: "bob@x.com", role: "TEACHER" },
      updatedAt: pastDate(500),
    })
  );

  // Bob submits joinRequest
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

  // Outsider Eve cannot list joinRequests
  await assertFails(
    eveDb.collection("schools").doc("school_2").collection("joinRequests").where("schoolAdminUids", "array-contains", ALICE_UID).get()
  );

  // Admin Alice approves Bob with specific subject permissions and deletes request
  const approveBatch = aliceDb.batch();
  approveBatch.update(aliceDb.collection("schools").doc("school_2"), {
    memberUids: [ALICE_UID, BOB_UID],
    "memberProfiles.bob_teacher_2": {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: false,
      allowedClasses: [3],
      allSubjects: false,
      allowedSubjects: ["MATH"],
      canViewOtherSubjects: false,
      canEditAttendance: false,
      canManageStudents: false,
      canExportPdf: false,
    },
    updatedAt: pastDate(300),
  });
  approveBatch.delete(aliceDb.collection("schools").doc("school_2").collection("joinRequests").doc(BOB_UID));
  await assertSucceeds(approveBatch.commit());
});

test("Granular teacher permissions restrict classes, subjects, attendance, and profile edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "KGP482", true);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: false,
      allowedClasses: [3],
      allSubjects: false,
      allowedSubjects: ["MATH"],
      canViewOtherSubjects: false,
      canEditAttendance: false,
      canManageStudents: false,
      canExportPdf: false,
    };
    await adminDb.collection("schools").doc("school_1").set(school);
    await adminDb.collection("schoolCodes").doc("KGP482").set(makeCodePayload("school_1", ALICE_UID));
  });

  // Admin creates Class 3 and Class 1 students
  const c3Ref = aliceDb.collection("schools").doc("school_1").collection("students").doc("st_c3");
  const c1Ref = aliceDb.collection("schools").doc("school_1").collection("students").doc("st_c1");
  await assertSucceeds(c3Ref.set(makeStudentPayload(ALICE_UID, 1, "নুসরাত", 3)));
  await assertSucceeds(c1Ref.set(makeStudentPayload(ALICE_UID, 1, "তানভীর", 1)));

  // Bob CAN update MATH marks in Class 3
  const bobC3 = bobDb.collection("schools").doc("school_1").collection("students").doc("st_c3");
  await assertSucceeds(
    bobC3.update({
      "m.MATH_1_TB": 9,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(300),
    })
  );

  // Bob CANNOT update BAN marks in Class 3
  await assertFails(
    bobC3.update({
      "m.BAN_1_TB": 5,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(250),
    })
  );

  // Bob CANNOT update Class 1 student (not in allowedClasses)
  await assertFails(
    bobDb.collection("schools").doc("school_1").collection("students").doc("st_c1").update({
      "m.MATH_1_TB": 15,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(200),
    })
  );

  // Bob CANNOT edit attendance or student name or delete student
  await assertFails(
    bobC3.update({
      "x.DAYS_1": "60",
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(150),
    })
  );
  await assertFails(
    bobC3.update({
      name: "Changed",
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(120),
    })
  );
  await assertFails(bobC3.delete());
});

test("Admin can configure custom classes and teachers with custom class permission can edit only those classes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "CST600", true);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: false,
      allowedClasses: [6],
      allSubjects: true,
      allowedSubjects: ["BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART"],
      canViewOtherSubjects: true,
      canEditAttendance: true,
      canManageStudents: true,
      canExportPdf: true,
    };
    await adminDb.collection("schools").doc("school_custom").set(school);
    await adminDb.collection("schoolCodes").doc("CST600").set(makeCodePayload("school_custom", ALICE_UID));
  });

  // Admin can update customClasses
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_custom").update({
      customClasses: [{ id: 6, nameBn: "৬ষ্ঠ", shortBn: "৬ষ্ঠ", syllabusTier: 3 }],
      "teachers.6": "নতুন শিক্ষক",
      "teacherMobiles.6": "01799999999",
      updatedAt: pastDate(300),
    })
  );

  // Admin can create Class 6 student, while cls 21 fails
  const c6Ref = aliceDb.collection("schools").doc("school_custom").collection("students").doc("st_c6");
  const c1Ref = aliceDb.collection("schools").doc("school_custom").collection("students").doc("st_c1");
  await assertSucceeds(c6Ref.set(makeStudentPayload(ALICE_UID, 1, "সাদিয়া", 6)));
  await assertSucceeds(c1Ref.set(makeStudentPayload(ALICE_UID, 1, "রাফি", 1)));
  await assertFails(
    aliceDb.collection("schools").doc("school_custom").collection("students").doc("st_c21")
      .set(makeStudentPayload(ALICE_UID, 1, "অবৈধ", 21))
  );

  // Teacher Bob (allowedClasses: [6]) can update Class 6 student, cannot update Class 1 student
  await assertSucceeds(
    bobDb.collection("schools").doc("school_custom").collection("students").doc("st_c6").update({
      "m.BAN_1_TB": 15,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(
    bobDb.collection("schools").doc("school_custom").collection("students").doc("st_c1").update({
      "m.BAN_1_TB": 15,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(150),
    })
  );
});

test("Subject-restricted teacher can update ART 4-components, CONT, and legacy TOTAL only for allowed subject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "ART202", true);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: false,
      allowedClasses: [2],
      allSubjects: false,
      allowedSubjects: ["ART", "PE"],
      canViewOtherSubjects: false,
      canEditAttendance: false,
      canManageStudents: false,
      canExportPdf: false,
    };
    await adminDb.collection("schools").doc("school_art").set(school);
    await adminDb.collection("schoolCodes").doc("ART202").set(makeCodePayload("school_art", ALICE_UID));
  });

  const c2Ref = aliceDb.collection("schools").doc("school_art").collection("students").doc("st_c2");
  await assertSucceeds(c2Ref.set(makeStudentPayload(ALICE_UID, 1, "মিমি", 2)));

  const bobC2 = bobDb.collection("schools").doc("school_art").collection("students").doc("st_c2");
  await assertSucceeds(
    bobC2.update({
      "m.ART_1_TB": 18,
      "m.ART_1_ACT": 4,
      "m.ART_1_LANG": 4,
      "m.ART_1_CT": 18,
      "m.PE_1_CONT": 46,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    bobC2.update({
      "m.ART_2_CONT": 45,
      "m.ART_3_TOTAL": 44,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(150),
    })
  );
  await assertFails(
    bobC2.update({
      "m.BAN_1_CONT": 40,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(100),
    })
  );
});

test("School members can sync and fetch thin-QR sheetTemplates while outsiders and spoofed writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "TPL100", true);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: true,
      allowedClasses: [1, 2, 3, 4, 5],
      allSubjects: true,
      allowedSubjects: ["BAN", "ENG", "MATH", "SOC", "SCI", "BGW", "REL", "PE", "ART"],
      canViewOtherSubjects: true,
      canEditAttendance: true,
      canManageStudents: true,
      canExportPdf: true,
    };
    await adminDb.collection("schools").doc("school_tpl").set(school);
    await adminDb.collection("schoolCodes").doc("TPL100").set(makeCodePayload("school_tpl", ALICE_UID));
  });

  const tid = "K9F2X8M1Q4";
  const tplPayload = (uid) => ({
    templateId: tid,
    cls: 3,
    subjectKey: "BAN",
    term: 1,
    sheetKind: "SINGLE_TERM_MARKS",
    importScope: "BOTH",
    columnIds: ["ROLL", "NAME", "TB", "ACT", "LANG", "CT", "CONT", "WR", "TOTAL", "CUS:0"],
    customColumnTitles: ["মৌখিক"],
    numeralStyle: "BENGALI",
    showSubjectMaxInHeader: true,
    workingDaysByTerm: { "1": "62" },
    pageIndex: 1,
    totalPages: 2,
    refCode: "REF:C3-BAN-T1-FULL",
    createdByUid: uid,
    updatedByUid: uid,
    createdAt: pastDate(2000),
    updatedAt: pastDate(2000),
  });

  const aliceTplRef = aliceDb.collection("schools").doc("school_tpl").collection("sheetTemplates").doc(tid);
  const bobTplRef = bobDb.collection("schools").doc("school_tpl").collection("sheetTemplates").doc(tid);
  const eveTplRef = eveDb.collection("schools").doc("school_tpl").collection("sheetTemplates").doc(tid);

  // Unauthenticated and outsider get/write must fail
  await assertFails(unauthDb.collection("schools").doc("school_tpl").collection("sheetTemplates").doc(tid).get());
  await assertFails(eveTplRef.set(tplPayload(EVE_UID)));

  // Spoofed createdByUid or ghost field must fail
  await assertFails(bobTplRef.set(tplPayload(ALICE_UID)));
  await assertFails(aliceTplRef.set({ ...tplPayload(ALICE_UID), isAdminOverride: true }));

  // Alice creates valid sheetTemplate; Bob (same school member) can get it by ID
  await assertSucceeds(aliceTplRef.set(tplPayload(ALICE_UID)));
  await assertSucceeds(bobTplRef.get());

  // Alice and Bob (school members) can list templates for offline pre-cache; outsider Eve and unauthenticated fail
  await assertSucceeds(aliceDb.collection("schools").doc("school_tpl").collection("sheetTemplates").limit(200).get());
  await assertSucceeds(bobDb.collection("schools").doc("school_tpl").collection("sheetTemplates").limit(200).get());
  await assertFails(eveTplRef.get());
  await assertFails(eveDb.collection("schools").doc("school_tpl").collection("sheetTemplates").get());
  await assertFails(unauthDb.collection("schools").doc("school_tpl").collection("sheetTemplates").get());

  // Bob cannot delete Alice's template, but admin Alice can
  await assertFails(bobTplRef.delete());
  await assertSucceeds(aliceTplRef.delete());
});

test("Admin can configure resultRules on school and subject-restricted teacher can toggle ABS_* only for allowed subjects", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const school = makeSchoolPayload(ALICE_UID, "RUL700", true);
    school.memberUids = [ALICE_UID, BOB_UID];
    school.memberProfiles[BOB_UID] = {
      uid: BOB_UID,
      name: "Bob",
      email: "bob@x.com",
      role: "TEACHER",
      allClasses: false,
      allowedClasses: [3],
      allSubjects: false,
      allowedSubjects: ["MATH"],
      canViewOtherSubjects: false,
      canEditAttendance: false,
      canManageStudents: false,
      canExportPdf: false,
    };
    await adminDb.collection("schools").doc("school_rules").set(school);
    await adminDb.collection("schoolCodes").doc("RUL700").set(makeCodePayload("school_rules", ALICE_UID));
  });

  // Admin can update valid resultRules, while non-map or non-admin fails
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_rules").update({
      resultRules: {
        attendanceRuleEnabled: true,
        minAttendancePct: 85.0,
        showFailIndicator: true,
        failIndicatorStyle: "CROSS",
        specialNeedsRuleEnabled: true,
        jointMeritEnabled: true,
      },
      updatedAt: pastDate(300),
    })
  );
  await assertFails(
    aliceDb.collection("schools").doc("school_rules").update({
      resultRules: "invalid",
      updatedAt: pastDate(250),
    })
  );
  await assertFails(
    bobDb.collection("schools").doc("school_rules").update({
      resultRules: { attendanceRuleEnabled: false },
      updatedAt: pastDate(240),
    })
  );

  // Admin creates Class 3 student
  const c3Ref = aliceDb.collection("schools").doc("school_rules").collection("students").doc("st_r3");
  await assertSucceeds(c3Ref.set(makeStudentPayload(ALICE_UID, 1, "সাজিদ", 3)));

  const bobC3 = bobDb.collection("schools").doc("school_rules").collection("students").doc("st_r3");

  // Bob (MATH only) can mark & clear ABS_MATH_1_WR, but NOT ABS_BAN_1_WR or SPECIAL_NEEDS
  await assertSucceeds(
    bobC3.update({
      "x.ABS_MATH_1_WR": "1",
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    bobC3.update({
      "x.ABS_MATH_1_WR": null,
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(180),
    })
  );
  await assertFails(
    bobC3.update({
      "x.ABS_BAN_1_WR": "1",
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(160),
    })
  );
  await assertFails(
    bobC3.update({
      "x.SPECIAL_NEEDS": "1",
      updatedByUid: BOB_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(140),
    })
  );
});



