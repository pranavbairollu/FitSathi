const fs = require("fs");
const path = require("path");
const minimist = require("minimist");
const OpenAI = require("openai");

// Parse command-line arguments
const args = minimist(process.argv.slice(2));
const filePath = args.file;
const task = args.task;

if (!filePath || !task) {
  console.error("Usage: node index.js --file <path-to-file> --task <your-task>");
  process.exit(1);
}

// Read the file
let code;
try {
  code = fs.readFileSync(filePath, "utf-8");
} catch (err) {
  console.error("Error reading file:", err.message);
  process.exit(1);
}

// Initialize OpenAI client
const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY, // Make sure to set this environment variable
});

async function modifyCode() {
  try {
    const prompt = `
You are a helpful AI assistant for Android Studio projects.
Here is the Java code of a file:

${code}

Task: ${task}

Return only the full updated Java code with your modifications.
`;

    const response = await client.chat.completions.create({
      model: "gpt-4.1-mini",
      messages: [{ role: "user", content: prompt }],
      max_tokens: 3000,
    });

    const modifiedCode = response.choices[0].message.content;

    fs.writeFileSync(filePath, modifiedCode, "utf-8");
    console.log("✅ File updated successfully!");
  } catch (err) {
    console.error("Error calling OpenAI API:", err);
  }
}

modifyCode();
