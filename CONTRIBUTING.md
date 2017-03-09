# Welcome!

If you are reading this document then you are interested in contributing to the Islandora CLAW, and Alpaca. All contributions are welcome: use-cases, documentation, code, patches, bug reports, feature requests, etc. You do not need to be a programmer to speak up!

## Workflows

The group meets each Wednesday at 1PM Eastern. Meeting notes and announcements are posted to the [Islandora community list](https://groups.google.com/forum/#!forum/islandora) and the [Islandora developers list](https://groups.google.com/forum/#!forum/islandora-dev). You can view meeting agendas, notes, and call-in information [here](https://github.com/Islandora-CLAW/CLAW/wiki#islandora-claw-tech-calls). Anybody is welcome to join the calls, and add items to the agenda.

### Use cases

If you would like to submit a use case for the Islandora CLAW project, please submit and issue [here](https://github.com/Islandora-CLAW/CLAW/issues/new) using the [Use Case template](https://github.com/Islandora-CLAW/CLAW/wiki/Use-Case-template), assign the "use case" label to the issue.

### Documentation

You can contribute documentation in two different ways. One way is to create an issue [here](https://github.com/Islandora-CLAW/CLAW/issues/new) assign the "documentation" label to the issue. Another way is to by pull request, same process as [Contribute Code](https://github.com/Islandora-CLAW/CLAW/blob/master/CONTRIBUTING.md#contribute-code). All documentation resides in [`docs`](https://github.com/Islandora-CLAW/CLAW/tree/master/docs).


### Request a new feature

To request a new feature you should [open an issue](https://github.com/Islandora-CLAW/CLAW/issues/new) or create a [use case](https://github.com/Islandora-CLAW/CLAW/blob/master/CONTRIBUTING.md#use-cases) (see _use case_ section above), and summarize the desired functionality. Select the label "enhancement" if creating an issue on the project repo, and "use case" if creating a use case in the interest group repo.

### Report a bug

To report a bug you should [open an issue](https://github.com/Islandora-CLAW/CLAW/issues/new) that summarizes the bug. Set the label to "bug".

In order to help us understand and fix the bug it would be great if you could provide us with:

1. The steps to reproduce the bug. This includes information about e.g. the Islandora version you were using along with version of stack components.
2. The expected behavior.
3. The actual, incorrect behavior.

Feel free to search the issue queue for existing issues (aka tickets) that already describe the problem; if there is such a ticket please add your information as a comment.

**If you want to provide a pull along with your bug report:**

That is great! In this case please send us a pull request as described in section _[Create a pull request](https://github.com/Islandora-CLAW/CLAW/blob/master/CONTRIBUTING.md#create-a-pull-request)_ below.

### Contribute code

Before you set out to contribute code you will need to have completed a [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or be covered by a [Corporate Contributor Licencse Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). The signed copy of the license agreement should be sent to <mailto:community@islandora.ca>

_If you are interested in contributing code to Islandora but do not know where to begin:_

In this case you should [browse open issues](https://github.com/Islandora-CLAW/CLAW/issues).

If you are contributing Drupal code, it must adhere to [Drupal Coding Standards](https://www.drupal.org/coding-standards); Travis CI will check for this on pull requests.

Contributions to the Islandora codebase should be sent as GitHub pull requests. See section _Create a pull request_ below for details. If there is any problem with the pull request we can work through it using the commenting features of GitHub.

* For _small patches_, feel free to submit pull requests directly for those patches.
* For _larger code contributions_, please use the following process. The idea behind this process is to prevent any wasted work and catch design issues early on.

    1. [Open an issue](https://github.com/Islandora-CLAW/CLAW/issues) and assign it the label of "enhancement", if a similar issue does not exist already. If a similar issue does exist, then you may consider participating in the work on the existing issue.
    2. Comment on the issue with your plan for implementing the issue. Explain what pieces of the codebase you are going to touch and how everything is going to fit together.
    3. Islandora committers will work with you on the design to make sure you are on the right track.
    4. Implement your issue, create a pull request (see below), and iterate from there.

### Create a pull request

Take a look at [Creating a pull request](https://help.github.com/articles/creating-a-pull-request). In a nutshell you need to:

1. [Fork](https://help.github.com/articles/fork-a-repo) the Islandora GitHub repository at [https://github.com/Islandora-CLAW/CLAW](https://github.com/Islandora-CLAW/CLAW) to your personal GitHub account. If you already have a fork of [https://github.com/Islandora/islandora](https://github.com/Islandora/islandora) Github will prevent you from creating a new fork; in such a case you can continue to use [https://github.com/Islandora/islandora](https://github.com/Islandora/islandora) to issue pull request, be cautious of which branches you work from though (you'll want to base your work off of master). Also be cautious of which repository your issuing your Pull Request to (you'll want to issue your pull request to [https://github.com/Islandora-CLAW/CLAW](https://github.com/Islandora-CLAW/CLAW)). See [Fork a repo](https://help.github.com/articles/fork-a-repo) for detailed instructions.
2. Commit any changes to your fork.
3. Send a [pull request](https://help.github.com/articles/creating-a-pull-request) to the Islandora GitHub repository that you forked in step 1.  If your pull request is related to an existing issue -- for instance, because you reported a [bug/issue](https://github.com/Islandora-CLAW/CLAW/issues) earlier -- prefix the title of your pull request with the corresponding issue number (e.g. `issue-123: ...`). Please also include a reference to the issue in the description of the pull. This can be done by using '#' plus the issue number like so '#123', also try to pick an appropriate name for the branch in which you're issuing the pull request from.

You may want to read [Syncing a fork](https://help.github.com/articles/syncing-a-fork) for instructions on how to keep your fork up to date with the latest changes of the upstream (official) `islandora` repository.

Please note that TravisCI will test for [PSR-2](http://www.php-fig.org/psr/psr-2/) compliance. You can verify coding standard compliance with [PHP Codesniffer](https://github.com/squizlabs/PHP_CodeSniffer).

**Note**: Due to differing interpretations of the PSR-2 coding standards do **not** use PHP [Coding Standards Fixer](http://cs.sensiolabs.org/). Coding Standards Fixer may make changes to your code that can cause it to fail our TravisCI tests.

In addition, Islandora CLAW Committers will review contributions for [PSR-4 ](http://www.php-fig.org/psr/psr-4/) compliance.

## License Agreements

The Islandora Foundation requires that contributors complete a [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or be covered by a [Corporate Contributor Licencse Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). The signed copy of the license agreement should be sent to <a href="mailto:community@islandora.ca?Subject=Contributor%20License%20Agreement" target="_top">community@islandora.ca</a>. This license is for your protection as a contributor as well as the protection of the Foundation and its users; it does not change your rights to use your own contributions for any other purpose.

