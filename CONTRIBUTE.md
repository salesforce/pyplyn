# Contributing to Pyplyn


### What contributions do we accept?

We are open to any contributions (code, artwork, documentation, etc.) as long as it respects our [licensing 
 requirements](#licensing-requirements).

**All contributors are required to sign the Salesforce Contributor's License Agreement.** 
Please [print this file](docs/SFDC_CLA.pdf), sign and scan it, and send it to [osscore@salesforce.com](mailto:osscore@salesforce.com).

We look forward to collaborating with you!


### Branching model and contribution process

Pyplyn loosely follows the "[gitflow](https://github.com/nvie/gitflow)" model, which is excellently explained on 
  [nvie.com](http://nvie.com/posts/a-successful-git-branching-model/).
 
We use the following branches:
 
 * **master**: always releasable, production-ready code
 * **develop**: code currently in development, that is to be delivered in the next release
 
Depending on each situation, we might decide to create *feature*, *release* or *hotfix* branches, or we might choose to
  merge PRs directly into these two branches.
 
What's important to you, the contributor is how you should approach contributing code or documentation content.

 1. Create an issue on GitHub so we can track what you're trying to change/add and ensure someone else isn't already 
    working on the same thing (be sure to check "Open issues" before opening your own)
 2. Fork the repository and clone it on your machine
 3. [Configure](https://help.github.com/articles/configuring-a-remote-for-a-fork/) your repo as a fork
 4. Checkout the _develop_ branch
 5. Ideally, branch out into a feature branch (unless you know you are making one small change in one commit)
 6. Write your code, test it (manually and with unit/integration tests), and commit it (obviously, repeat this step 
    as many times as you need to)
 7. [Sync your fork](https://help.github.com/articles/syncing-a-fork/) (_develop_ branch) with the upstream repository 
 8. Rebase your feature branch on the current _develop_ HEAD
 9. Squash your commits in the feature branch and merge it back into develop
10. Manually test your change one last time
11. Create a Pull Request and submit it for review


### Filing bugs

If you find any bugs in *Pyplyn*, please open a new issue on GitHub and add as many details as possible, but at least the following:

- Expected outcome
- Actual outcome
- Is reproducible? (Always, Randomly, etc.)
- Reproducer scenario
- Your dev environment (Linux, Mac OS, version, etc.)


### Contribution / bug review SLA

After submitting a bug or contribution, you can expect to hear back an answer within the next five working days.


### Setting up your environment
 
Before you do anything else, configure your git client to "__rebase__ on pull". 

The __autoStash__ setting is not required, but it will make your life easier.

```
git config --global pull.rebase true  # or always use 'git pull --rebase' to avoid unnecessary merges 
git config --global rebase.autoStash true  # optional
```


### Signing your commits

We require your commits to be signed (for security purposes).


1\. [Generate a new GPG key](https://help.github.com/articles/generating-a-new-gpg-key/)

**Note:** you might need to install gpg if you're using Mac OS:

```
brew install gpg
```


2\. [Add the generated GPG key](https://help.github.com/articles/adding-a-new-gpg-key-to-your-github-account/) to your GitHub account


3\. Configure the git client on your machine

```
# Obtain the secret key's identifier 
gpg --list-secret-keys --keyid-format LONG

# Set the signing key (replace *GPG_USER_KEY* with the key from above)
git config --global user.signingkey GPG_USER_KEY

# (Optional) Enable automatic signing of all commits
git config --global commit.gpgSign true
```

4\. After you've configured GPG signing and made your first commit, you can check that your commit was signed, with:

```
git show HEAD --show-signature
# or
git log --show-signature
```

**Note**: If you find that you can't sign & commit, add the following to your *.profile* or *.bashrc* files

```
export GPG_TTY=$(tty)
```


### Licensing requirements

All but the most trivial projects have dependencies on third party software libraries. 
Third party software licenses come in a wide variety of forms, each with different legal implications for Salesforce. 

We can only accept the following license types, and so, if you're adding new libraries in the project, please make sure
 to check that they conform to the following list:

* BSD
* Apache 2.0
* MIT
* ISC
* MPL

Note that all Pull Requests will be reviewed for any new dependencies and will be rejected if they do not fall into
 the above categories. If you must use a dependency with a different licensing model, an alternative is to
 extend Pyplyn, as explained in the [API reference](https://salesforce.github.io/pyplyn/#extending-pyplyn).
 
If you don't know how to check for licenses, have a look at Pivotal's [LicenseFinder](https://github.com/pivotal/LicenseFinder),
 which automatically scans all the project's files and compiles a list of all its dependencies and their corresponding
 license types.
 
Thank you for understanding!
