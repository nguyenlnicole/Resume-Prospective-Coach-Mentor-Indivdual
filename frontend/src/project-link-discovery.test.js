import test from "node:test";
import assert from "node:assert/strict";
import {
  matchProjectLinks,
  isLinkQuestion,
  discoverProjectLinks,
  readmeDemoLinks,
} from "./project-link-discovery.js";

test("matches a distinct project name despite a descriptive Web Page suffix", () => {
  const repo = {
    name: "EverPure",
    html_url: "https://github.com/person/EverPure",
    homepage: "https://everpure.example",
  };
  assert.deepEqual(
    matchProjectLinks(["EverPure Webpage — Java\nDetails"], [repo]),
    { 0: [repo.html_url, repo.homepage] },
  );
  assert.deepEqual(
    matchProjectLinks(
      ["EverPure Webpage"],
      [repo, { ...repo, name: "EverPureApp" }],
    ),
    {},
  );
});

test("uses a supplied repository URL even when the project was renamed", () => {
  const repo = {
    name: "pet-shelter",
    html_url: "https://github.com/person/pet-shelter",
    homepage: "https://pets.example",
  };
  assert.deepEqual(
    matchProjectLinks(
      ["Pet Adoption Platform | https://github.com/person/pet-shelter"],
      [repo],
    ),
    { 0: [repo.html_url, repo.homepage] },
  );
});

test("finds only clearly labelled demo destinations in a README", () => {
  assert.deepEqual(
    readmeDemoLinks(
      "[Live Demo](https://project.example)\n[Docs](https://docs.example)\n![Demo](https://images.example/a.png)",
    ),
    ["https://project.example/"],
  );
});

test("profile lookup fills a matching repository and README demo", async () => {
  const originalFetch = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url) => {
    requests.push(url);
    return {
      ok: true,
      json: async () =>
        url.endsWith("/readme")
          ? {
              size: 80,
              content: Buffer.from(
                "[Live Demo](https://everpure.example)",
              ).toString("base64"),
            }
          : [
              {
                name: "EverPure",
                html_url: "https://github.com/person/EverPure",
                homepage: "",
              },
            ],
    };
  };
  try {
    assert.deepEqual(
      await discoverProjectLinks(
        ["EverPure Webpage"],
        "www.github.com/person",
        "",
      ),
      {
        0: ["https://github.com/person/EverPure", "https://everpure.example/"],
      },
    );
    assert.equal(requests.length, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
test("matches exact normalized names and includes code and demo", () => {
  assert.deepEqual(
    matchProjectLinks(
      ["Fit To Go — React\nDescription"],
      [
        {
          name: "fit-to-go",
          html_url: "https://github.com/person/fit-to-go",
          homepage: "https://fit.example",
        },
      ],
    ),
    { 0: ["https://github.com/person/fit-to-go", "https://fit.example"] },
  );
});
test("does not attach unrelated or ambiguous repositories", () => {
  assert.deepEqual(
    matchProjectLinks(
      ["App"],
      [{ name: "Other", html_url: "https://github.com/p/other" }],
    ),
    {},
  );
  assert.deepEqual(
    matchProjectLinks(["App"], [{ name: "app" }, { name: "App" }]),
    {},
  );
});

test("does not infer project ownership from a matching fork", () => {
  assert.deepEqual(
    matchProjectLinks(
      ["Pet Shelter"],
      [
        {
          name: "PetShelter",
          fork: true,
          html_url: "https://github.com/person/PetShelter",
        },
      ],
    ),
    {},
  );
});
test("unassigned demo cannot be guessed across multiple projects", () => {
  assert.deepEqual(
    matchProjectLinks(["App", "Other"], [], "https://demo.example"),
    {},
  );
  assert.deepEqual(matchProjectLinks(["App"], [], "https://demo.example"), {
    0: ["https://demo.example"],
  });
});
test("URL questions are optional without weakening other questions", () => {
  assert.equal(
    isLinkQuestion({ question: "Provide your repository URL", required: true }),
    true,
  );
  assert.equal(
    isLinkQuestion({ question: "What is your job title?", required: true }),
    false,
  );
});
