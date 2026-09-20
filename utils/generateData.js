const { faker } = require('@faker-js/faker');

const N = 10_000; // Number of customers
const M = 100_000; // Target minimum number of orders

// Escape single quotes for safe embedding in a SQL string literal.
// Faker names/descriptions can contain apostrophes (e.g. "O'Brien").
function sqlString(value) {
    return value.replace(/'/g, "''");
}

// Realistic order-count distribution: most customers place a handful of
// orders, a long tail places none or one, and a small number of "whale"
// customers place hundreds. A uniform random assignment (the original
// approach) converges to every customer having ~M/N orders with almost no
// spread, which doesn't exercise skew-sensitive query plans.
function assignOrderCount() {
    const roll = Math.random();
    if (roll < 0.01) return faker.datatype.number({ min: 150, max: 450 }); // ~1% whales
    if (roll < 0.06) return faker.datatype.number({ min: 40, max: 149 }); // ~5% heavy
    if (roll < 0.26) return faker.datatype.number({ min: 5, max: 39 }); // ~20% regular
    if (roll < 0.56) return faker.datatype.number({ min: 1, max: 4 }); // ~30% light, incl. one-order customers
    return faker.datatype.number({ min: 0, max: 1 }); // ~44% minimal/no orders
}

// Generate customers
for (let i = 1; i <= N; i++) {
    console.log(`INSERT INTO customer (id, name) VALUES (${i}, '${sqlString(faker.name.fullName())}');`);
}

// Generate orders: decide each customer's order count up front from the
// skewed distribution above, then emit that many rows for them. Order IDs
// are assigned sequentially as rows are emitted, so the final count can
// exceed M slightly depending on the random draw - that's fine, M is a
// floor, not an exact target.
let orderId = 1;
let totalOrders = 0;
for (let customerId = 1; customerId <= N; customerId++) {
    const orderCount = assignOrderCount();
    for (let j = 0; j < orderCount; j++) {
        console.log(
            `INSERT INTO "order" (id, description, customer_id) VALUES (${orderId}, '${sqlString(faker.commerce.productName())}', ${customerId});`,
        );
        orderId++;
    }
    totalOrders += orderCount;
}

// Top up with evenly-spread orders if the random draw fell short of the
// M floor, so the dataset reliably meets the "at least 100,000 orders"
// target regardless of how the dice landed.
while (totalOrders < M) {
    const customerId = Math.ceil(Math.random() * N);
    console.log(
        `INSERT INTO "order" (id, description, customer_id) VALUES (${orderId}, '${sqlString(faker.commerce.productName())}', ${customerId});`,
    );
    orderId++;
    totalOrders++;
}

console.error(`Generated ${N} customers and ${totalOrders} orders.`);
