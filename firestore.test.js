const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function makeTaskPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId,
    title: "Plan weekly groceries",
    notes: "Pick up fresh greens and oat milk",
    completed: false,
    priority: "MEDIUM",
    category: "ERRANDS",
    dueAtMillis: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
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

test("Unauthenticated user cannot list, read, create, or delete tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/tasks/task_1`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/tasks/task_1`).set(makeTaskPayload(ALICE_UID))
  );
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/tasks/task_1`).delete());
});

test("Authenticated owner can create, list, toggle, edit, and delete valid Kotlin-mirrored tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksCol = aliceDb.collection(`users/${ALICE_UID}/tasks`);
  const taskRef = tasksCol.doc("task_1");

  // List empty collection succeeds (no existing() inspection on list)
  await assertSucceeds(tasksCol.get());

  // Create valid task (no @DocumentId `id` field)
  await assertSucceeds(taskRef.set(makeTaskPayload(ALICE_UID)));

  // List with orderBy matching Kotlin repository query
  await assertSucceeds(tasksCol.orderBy("createdAt", "desc").get());

  // Toggle completion partial update
  await assertSucceeds(
    taskRef.update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full detail edit partial update (preserving userId and createdAt)
  await assertSucceeds(
    taskRef.update({
      title: "Plan weekly organic groceries",
      notes: "Add sourdough bread",
      completed: false,
      priority: "HIGH",
      category: "PERSONAL",
      dueAtMillis: Date.now() + 86400000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete succeeds
  await assertSucceeds(taskRef.delete());
});

test("Rejects cross-user reads, writes, lists, and deletes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .doc(`users/${BOB_UID}/tasks/bob_task`)
      .set(makeTaskPayload(BOB_UID, { title: "Bob secret task" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/tasks`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/tasks/bob_task`).get());
  await assertFails(
    aliceDb.doc(`users/${BOB_UID}/tasks/bob_task`).update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${BOB_UID}/tasks/new_task`).set(makeTaskPayload(ALICE_UID))
  );
  await assertFails(aliceDb.doc(`users/${BOB_UID}/tasks/bob_task`).delete());
});

test("Rejects @DocumentId id field, ghost fields, invalid enums, oversized strings, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksCol = aliceDb.collection(`users/${ALICE_UID}/tasks`);

  // Reject redundant `id` field
  await assertFails(
    tasksCol.doc("task_id_field").set(makeTaskPayload(ALICE_UID, { id: "task_id_field" }))
  );

  // Reject ghost field
  await assertFails(
    tasksCol.doc("task_ghost").set(makeTaskPayload(ALICE_UID, { isAdmin: true }))
  );

  // Reject blank or oversized title
  await assertFails(
    tasksCol.doc("task_empty_title").set(makeTaskPayload(ALICE_UID, { title: "" }))
  );
  await assertFails(
    tasksCol.doc("task_long_title").set(makeTaskPayload(ALICE_UID, { title: "a".repeat(121) }))
  );

  // Reject invalid priority / category enum
  await assertFails(
    tasksCol.doc("task_bad_prio").set(makeTaskPayload(ALICE_UID, { priority: "URGENT" }))
  );
  await assertFails(
    tasksCol.doc("task_bad_cat").set(makeTaskPayload(ALICE_UID, { category: "FINANCE" }))
  );

  // Reject negative dueAtMillis
  await assertFails(
    tasksCol.doc("task_neg_due").set(makeTaskPayload(ALICE_UID, { dueAtMillis: -1 }))
  );

  // Reject future timestamp
  await assertFails(
    tasksCol
      .doc("task_future")
      .set(makeTaskPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Create valid task and verify immutable fields cannot be modified
  await assertSucceeds(tasksCol.doc("task_valid").set(makeTaskPayload(ALICE_UID)));
  await assertFails(
    tasksCol.doc("task_valid").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    tasksCol.doc("task_valid").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
