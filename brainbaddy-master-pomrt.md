# Alpha Net – GEO Performance & Improvement Report

**Website:** www.alpha.net.bd  
**Reporting Period:** 29 September – 26 October 2026  
**Prepared For:** Web Head  
**Subject:** Current GEO Performance, AI Traffic Tracking & Improvement Plan

## 1. Current GEO Performance

Google Analytics (GA4) অনুযায়ী গত 28 দিনে Alpha Net-এর ওয়েবসাইটে ChatGPT-attributed Traffic-এর ফলাফল:

| Metric | Current Result |
|---|---:|
| Total Sessions | 655 |
| Active Users | 319 |
| New Users | 267 |
| Engaged Sessions | 504 |
| Engagement Rate | 76.95% |
| Average Engagement Time | 1m 37s |
| Key Events | 35 |
| Reported Revenue | $93.69 |

**Current Status:** ChatGPT থেকে ইতোমধ্যে Traffic এবং Conversion Activity পাওয়া যাচ্ছে। তবে Gemini, Claude এবং অন্যান্য AI Platform-এর Traffic এখনও আলাদাভাবে যাচাই করা হয়নি।

## 2. Top Landing Pages from ChatGPT

| Landing Page | Sessions |
|---|---:|
| Homepage | 138 |
| Client Area | 37 |
| Careers | 35 |
| Python Developer Job | 29 |
| .COM Domain | 26 |
| Customer Service Job | 20 |
| SMS Service | 17 |
| SMS API | 16 |
| Technical Support Job | 14 |

এছাড়া 65টি Session-এর Landing Page `(not set)` দেখাচ্ছে, যা পরীক্ষা করা প্রয়োজন।

**Finding:** Homepage, Domain, Career এবং SMS Pages-এ ChatGPT-attributed Traffic আসছে। `.COM Domain` পেজে 13টি Key Event পাওয়া গেছে।

## 3. How to Identify Traffic from ChatGPT, Gemini & Claude

AI Platform থেকে Visitor সাধারণত এভাবে আসে:

1. একজন ব্যবহারকারী ChatGPT, Gemini বা Claude-এ কোনো Service সম্পর্কে প্রশ্ন করেন।
2. AI Platform তার উত্তরে Alpha Net-এর Website Link দেখাতে পারে।
3. ব্যবহারকারী সেই Link-এ ক্লিক করে Alpha Net Website-এ প্রবেশ করেন।
4. GA4 Referrer অথবা UTM Parameter-এর মাধ্যমে Traffic Source শনাক্ত করতে পারে।

### AI Platform Identification

| AI Platform | Tracking Source |
|---|---|
| ChatGPT | `chatgpt.com` |
| Google Gemini | `gemini.google.com` |
| Claude | `claude.ai` |
| Perplexity | `perplexity.ai` |
| Microsoft Copilot | `copilot.microsoft.com` |
| DeepSeek | `chat.deepseek.com` |

### How to Check in GA4

**Step 1:** Google Analytics → Reports → Acquisition → Traffic acquisition

**Step 2:** Primary Dimension পরিবর্তন করে `Session source / medium` নির্বাচন করুন।

**Step 3:** Search বা Filter-এ পর্যায়ক্রমে `chatgpt`, `gemini`, `claude`, `perplexity` লিখে Traffic পরীক্ষা করুন।

**Step 4:** Sessions, Active Users, Engagement, Key Events এবং Revenue তুলনা করুন।

**Step 5:** Landing Page Report-এ একই Source Filter ব্যবহার করে কোন AI Platform থেকে কোন Service Page-এ Visitor আসছে তা বের করুন।

**Important:** GA4-এর `AI Assistant` Channel-ও পরীক্ষা করতে হবে। কিছু AI Traffic `Referral`, `Unassigned` বা `Direct` হিসেবে থাকতে পারে। তাই সব AI Traffic শতভাগ শনাক্ত করা সম্ভব নয়।

## 4. How to Find Which Questions Recommend Alpha Net

GA4 দিয়ে ChatGPT বা Gemini-তে ব্যবহারকারী ঠিক কী প্রশ্ন করেছিলেন, তা সরাসরি জানা যায় না।

এজন্য আলাদা **AI Visibility Testing** করতে হবে।

### Testing Process

প্রতি মাসে ChatGPT, Gemini, Claude এবং Perplexity-তে নিচের ধরনের প্রশ্ন পরীক্ষা করতে হবে:

- Best web hosting company in Bangladesh
- Best domain registration provider in Bangladesh
- Reliable VPS hosting provider in Bangladesh
- Dedicated server provider in Bangladesh
- Bulk SMS API provider in Bangladesh
- Secure data center hosting in Bangladesh

প্রতিটি Platform-এ যাচাই করতে হবে:

- Alpha Net-এর নাম এসেছে কি না।
- Website Link বা Citation দেখাচ্ছে কি না।
- কোন Service Page Recommend করছে।
- প্রতিযোগী কোন কোন Company দেখাচ্ছে।
- Alpha Net সম্পর্কে দেওয়া তথ্য সঠিক কি না।

### Monthly AI Visibility Tracking

| Platform | Brand Mention | Website Citation | Position/Observation |
|---|---|---|---|
| ChatGPT | To Check | To Check | Pending |
| Gemini | To Check | To Check | Pending |
| Claude | To Check | To Check | Pending |
| Perplexity | To Check | To Check | Pending |

**Note:** AI উত্তর ব্যক্তিভেদে ও সময়ভেদে পরিবর্তিত হতে পারে। তাই একই প্রশ্ন নিয়মিত পরীক্ষা করে ফলাফল সংরক্ষণ করতে হবে। এটি নির্দিষ্ট বা স্থায়ী Ranking নয়।

## 5. GEO Improvement Requirements

### A. Service Content Optimization

- Hosting, Domain, VPS, Dedicated Server ও SMS Service Content আপডেট করা।
- প্রতিটি Service Page-এ Features, Specifications, Benefits ও FAQ যুক্ত করা।
- Bangladesh-specific Search Questions অনুযায়ী Content তৈরি করা।
- পুরোনো Pricing, ভুল Information ও Duplicate Content সংশোধন করা।

### B. Technical SEO & GEO

- XML Sitemap ও Robots.txt পরীক্ষা করা।
- গুরুত্বপূর্ণ Page-এর Indexing এবং Crawl Access নিশ্চিত করা।
- Relevant Schema Markup ও Canonical URL ব্যবহার করা।
- Page Speed ও Mobile Performance উন্নত করা।
- AI Search Crawlers-এর Access Policy পরীক্ষা করা।

### C. Brand Authority & Trust

- Company Profile, Certifications ও Business Documents আপডেট করা।
- Data Center Infrastructure এবং Security Information বিস্তারিত প্রকাশ করা।
- Customer Reviews, Case Studies ও Technical Articles যুক্ত করা।
- বিশ্বস্ত External Websites-এ Alpha Net-এর Brand Mentions বৃদ্ধি করা।

### D. Analytics & Conversion Tracking

- GA4-এ AI Platform অনুযায়ী Traffic Report তৈরি করা।
- Lead, Registration এবং Purchase Events আলাদাভাবে Track করা।
- `(not set)` Landing Page সমস্যার কারণ পরীক্ষা করা।
- AI Traffic থেকে Qualified Lead ও Sales পরিমাপ করা।

## 6. Recommended 30-Day Action Plan

| Timeline | Action |
|---|---|
| Week 1 | GA4 AI Traffic Tracking Setup, GEO Audit ও AI Visibility Baseline |
| Week 2 | Hosting, Domain, VPS, SMS ও Dedicated Server Content Optimization |
| Week 3 | Schema, Internal Linking, Technical SEO ও Brand Trust Improvements |
| Week 4 | ChatGPT, Gemini, Claude Testing এবং GA4 Performance Review |

## 7. Monthly GEO Monitoring

প্রতি মাসে Web Team নিচের বিষয়গুলো রিপোর্ট করবে:

1. ChatGPT, Gemini, Claude ও অন্যান্য AI Platform থেকে Sessions।
2. AI Platform অনুযায়ী Top Landing Pages।
3. AI Traffic থেকে Leads, Key Events ও Revenue।
4. নির্বাচিত Search Questions-এ Alpha Net-এর Mention ও Citation।
5. আগের মাসের তুলনায় Traffic ও Visibility পরিবর্তন।

## 8. Expected Outcome & Recommendation

Alpha Net ইতোমধ্যে ChatGPT থেকে Traffic পাচ্ছে। এটি GEO Development-এর জন্য একটি ইতিবাচক সূচনা।

পরবর্তী লক্ষ্য হবে:

- অন্যান্য AI Platform-এর Traffic শনাক্ত করা।
- Alpha Net-এর AI Search Visibility পরিমাপ করা।
- Service Pages-এর Discoverability উন্নত করা।
- AI Traffic থেকে Qualified Leads এবং Sales বৃদ্ধি করা।

**Recommendation:** প্রথমে GA4-এ AI Platform-wise Tracking এবং AI Visibility Baseline তৈরি করতে হবে। এরপর Content ও Technical GEO Improvements বাস্তবায়ন করে প্রতি মাসে ফলাফল তুলনা করতে হবে।

**Priority:** High  
**Responsible Team:** Web Development, SEO & Content Team  
**Review Frequency:** Monthly